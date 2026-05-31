package com.example.smartreceiptmanager.scanbill;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.databinding.FragmentScanBillBinding;
import com.example.smartreceiptmanager.expense.AutoCategoryFragment;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanBillFragment extends Fragment {

    private FragmentScanBillBinding binding;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private ImageCapture imageCapture;
    private boolean isFlashOn = false;
    private ObjectAnimator scanLineAnimator;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else binding.tvStatus.setText("Cần cấp quyền camera để quét hóa đơn");
            });
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleImageUri(uri);
            });
    private final ActivityResultLauncher<String> galleryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) galleryLauncher.launch("image/*");
                else binding.tvStatus.setText("Cần cấp quyền truy cập ảnh");
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScanBillBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();
        setupClickListeners();
        checkCameraPermission();
        startScanLineAnimation();
    }

    private void setupClickListeners() {

        binding.btnFlash.setOnClickListener(v -> {
            isFlashOn = !isFlashOn;
            if (camera != null) camera.getCameraControl().enableTorch(isFlashOn);
            binding.btnFlash.setImageResource(
                    isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        });

        binding.btnCapture.setOnClickListener(v -> captureImage());
        binding.btnGallery.setOnClickListener(v -> openGallery());
        binding.btnAutoManual.setOnClickListener(v -> { /* TODO toggle */ });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture);

                binding.tvStatus.setText("Đang tìm kiếm hóa đơn...");

            } catch (Exception e) {
                binding.tvStatus.setText("Không thể khởi động camera");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void captureImage() {
        if (imageCapture == null) return;
        binding.tvStatus.setText("Đang chụp...");

        File outputFile = new File(requireContext().getCacheDir(), "receipt.jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(options, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                        if (bitmap != null) {
                            ScanResultFragment.previewBitmap = bitmap;
                            requireActivity().runOnUiThread(() -> processImage(bitmap));
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        requireActivity().runOnUiThread(() ->
                                binding.tvStatus.setText("Lỗi chụp: " + exception.getMessage()));
                    }
                });
    }
    private void openGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*");
        } else {
            galleryPermissionLauncher.launch(permission);
        }
    }
    // phân tích ảnh hóa đơn
    private void processImage(Bitmap bitmap){
        binding.tvStatus.setText("Đang phân tích ảnh...");
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        BarcodeScanner barcodeScanner = BarcodeScanning.getClient();
        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        // Tìm thấy QR
                        handleBarcodeResult(barcodes.get(0), bitmap);
                    } else {
                        // Không có QR → thử OCR văn bản
                        processTextOCR(image, bitmap);
                    }
                })
                .addOnFailureListener(e -> processTextOCR(image, bitmap));


    }

    private void handleBarcodeResult(Barcode barcode, Bitmap bitmap) {
        String rawValue = barcode.getRawValue() != null ? barcode.getRawValue() : "";
        binding.tvStatus.setText("Đã quét QR");

        String shopName;
        long amount = 0;

        switch (barcode.getValueType()) {
            case Barcode.TYPE_URL:
                shopName = barcode.getUrl() != null
                        ? barcode.getUrl().getTitle() : "QR URL";
                break;
            case Barcode.TYPE_TEXT:
            default:
                shopName = parseShopName(rawValue);
                amount   = parseAmount(rawValue);
                break;
        }

        navigateToResult(shopName, amount, rawValue);
    }


    private void handleImageUri(Uri uri) {
        binding.tvStatus.setText("Đang xử lý ảnh...");

        // Chạy trên background thread tránh block UI
        cameraExecutor.execute(() -> {
            try {
                InputStream inputStream = requireContext()
                        .getContentResolver()
                        .openInputStream(uri);

                if (inputStream == null) {
                    requireActivity().runOnUiThread(() ->
                            binding.tvStatus.setText("Không mở được ảnh"));
                    return;
                }

                // Scale down nếu ảnh quá lớn tránh OOM
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                // Tính tỉ lệ scale
                options.inSampleSize = calculateInSampleSize(options, 1080, 1920);
                options.inJustDecodeBounds = false;

                // Mở lại stream lần 2 để decode thật
                InputStream inputStream2 = requireContext()
                        .getContentResolver()
                        .openInputStream(uri);

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream2, null, options);
                if (inputStream2 != null) inputStream2.close();

                if (bitmap == null) {
                    requireActivity().runOnUiThread(() ->
                            binding.tvStatus.setText("Không đọc được ảnh"));
                    return;
                }

                // Lưu bitmap để hiển thị trong ScanResultFragment
                ScanResultFragment.previewBitmap = bitmap;

                // Quay về main thread để xử lý tiếp
                final Bitmap finalBitmap = bitmap;
                requireActivity().runOnUiThread(() -> processImage(finalBitmap));

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        binding.tvStatus.setText("Lỗi: " + e.getMessage()));
            }
        });
    }
    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width  = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth  = width  / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth  / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    private void processTextOCR(InputImage image, Bitmap bitmap) {
        binding.tvStatus.setText("Đang nhận dạng văn bản...");

        TextRecognizer recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();
                    if (rawText.isEmpty()) {
                        binding.tvStatus.setText("Không tìm thấy nội dung");
                        return;
                    }
                    String shopName = parseShopName(rawText);
                    long amount     = parseAmount(rawText);
                    binding.tvStatus.setText("Đã nhận dạng xong");
                    navigateToResult(shopName, amount, rawText);
                })
                .addOnFailureListener(e ->
                        binding.tvStatus.setText("OCR thất bại: " + e.getMessage()));
    }
    private void navigateToResult(String shopName, long amount, String rawText) {
        ScanResultFragment fragment = ScanResultFragment.newInstance(
                shopName, amount, System.currentTimeMillis(), rawText);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ── Parse tên cửa hàng ────────────────────────────────────────
    private String parseShopName(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && line.length() > 2) return line;
        }
        return "Không rõ";
    }

    // ── Parse số tiền (lấy số lớn nhất) ──────────────────────────
    private long parseAmount(String text) {
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("\\d[\\d.,]{2,}");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        long maxAmount = 0;
        while (matcher.find()) {
            String raw = matcher.group()
                    .replace(".", "")
                    .replace(",", "");
            try {
                long val = Long.parseLong(raw);
                if (val > maxAmount) maxAmount = val;
            } catch (NumberFormatException ignored) {}
        }
        return maxAmount;
    }

    private void startScanLineAnimation() {
        binding.scanFrame.post(() -> {
            float frameHeight = binding.scanFrame.getHeight();
            scanLineAnimator = ObjectAnimator.ofFloat(
                    binding.scanLine, "translationY", 0f, frameHeight);
            scanLineAnimator.setDuration(2000);
            scanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
            scanLineAnimator.setRepeatMode(ValueAnimator.REVERSE);
            scanLineAnimator.setInterpolator(new LinearInterpolator());
            scanLineAnimator.start();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scanLineAnimator != null) scanLineAnimator.cancel();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        binding = null;
    }
}