package com.example.smartreceiptmanager.scanbill;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
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
        binding.btnBack.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new com.example.smartreceiptmanager.home.HomeFragment())
                    .commit();

            requireActivity().findViewById(R.id.custom_bottom_nav).setVisibility(View.VISIBLE);
        });

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
        //TEST Tạm
        if (imageCapture == null) return;

        binding.tvStatus.setText("Đang xử lý hóa đơn...");

        AutoCategoryFragment fragment = AutoCategoryFragment.newInstance(
                "Highlands Coffee",
                150000,
                System.currentTimeMillis(),
                "Highlands Coffee\nTổng cộng: 150000"
        );

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openGallery() {
        //TEST tạm
        binding.tvStatus.setText("Đang xử lý ảnh từ thư viện...");

        AutoCategoryFragment fragment = AutoCategoryFragment.newInstance(
                "Co.opmart",
                245000,
                System.currentTimeMillis(),
                "Co.opmart\nTổng thanh toán: 245000"
        );

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
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