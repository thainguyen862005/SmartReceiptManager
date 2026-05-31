package com.example.smartreceiptmanager.scanbill;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.databinding.FragmentScanResultBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScanResultFragment extends Fragment {

    private static final String ARG_SHOP    = "shop_name";
    private static final String ARG_AMOUNT  = "amount";
    private static final String ARG_DATE    = "date";
    private static final String ARG_RAW     = "raw_text";

    private FragmentScanResultBinding binding;

    // Bitmap giữ tạm trong bộ nhớ — sau này thay bằng URI/database
    public static Bitmap previewBitmap = null;

    // -------------------------------------------------------
    public static ScanResultFragment newInstance(
            String shopName, long amount, long dateMillis, String rawText) {

        Bundle args = new Bundle();
        args.putString(ARG_SHOP,   shopName);
        args.putLong  (ARG_AMOUNT, amount);
        args.putLong  (ARG_DATE,   dateMillis);
        args.putString(ARG_RAW,    rawText);

        ScanResultFragment f = new ScanResultFragment();
        f.setArguments(args);
        return f;
    }

    // -------------------------------------------------------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScanResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        String shopName  = args.getString(ARG_SHOP,   "Không rõ");
        long   amount    = args.getLong  (ARG_AMOUNT,  0);
        long   dateMs    = args.getLong  (ARG_DATE,    System.currentTimeMillis());
        String rawText   = args.getString(ARG_RAW,    "");

        // Hiển thị ảnh nếu có
        if (previewBitmap != null) {
            binding.imgReceipt.setImageBitmap(previewBitmap);
        }

        // Điền dữ liệu vào form
        binding.etShopName.setText(shopName);
        binding.etAmount.setText(String.valueOf(amount));
        binding.etDate.setText(
                new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        .format(new Date(dateMs)));

        // Chọn chip mặc định
        binding.chipEat.setChecked(true);

        // Xem ảnh lớn
        binding.btnViewLarge.setOnClickListener(v -> {
            if (previewBitmap == null) return;
            showFullImageDialog();
        });

        // Quét lại
        binding.btnRescan.setOnClickListener(v -> {
            previewBitmap = null;
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });

        // Lưu hóa đơn
        binding.btnSave.setOnClickListener(v -> {
            String finalShop   = binding.etShopName.getText() != null
                    ? binding.etShopName.getText().toString() : shopName;
            String finalAmount = binding.etAmount.getText() != null
                    ? binding.etAmount.getText().toString() : "0";
            String finalDate   = binding.etDate.getText() != null
                    ? binding.etDate.getText().toString() : "";

            int selectedChipId = binding.chipGroup.getCheckedChipId();
            String category = "Khác";
            if (selectedChipId == R.id.chipEat)    category = "Ăn uống";
            else if (selectedChipId == R.id.chipCoffee) category = "Cà phê";

            // TODO: truyền vào database sau
            // saveToDatabase(finalShop, finalAmount, finalDate, category, rawText);

            Toast.makeText(requireContext(),
                    "Đã lưu: " + finalShop + " - " + finalAmount + "đ",
                    Toast.LENGTH_SHORT).show();
        });
    }
    private void showFullImageDialog() {
        if (previewBitmap == null) return;

        android.app.Dialog dialog = new android.app.Dialog(
                requireContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(requireContext());
        frameLayout.setBackgroundColor(android.graphics.Color.BLACK);

        android.widget.ImageView imageView = new android.widget.ImageView(requireContext());
        imageView.setImageBitmap(previewBitmap);
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        imageView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        final float[] scale = {1f};
        final float[] lastX = {0f};
        final float[] lastY = {0f};

        android.view.ScaleGestureDetector scaleDetector =
                new android.view.ScaleGestureDetector(requireContext(),
                        new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            @Override
                            public boolean onScale(android.view.ScaleGestureDetector detector) {
                                scale[0] *= detector.getScaleFactor();
                                scale[0] = Math.max(1f, Math.min(scale[0], 5f));
                                imageView.setScaleX(scale[0]);
                                imageView.setScaleY(scale[0]);
                                return true;
                            }
                        });

        imageView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            if (event.getPointerCount() == 1 && scale[0] > 1f) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        lastX[0] = event.getX();
                        lastY[0] = event.getY();
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - lastX[0];
                        float dy = event.getY() - lastY[0];
                        imageView.setTranslationX(imageView.getTranslationX() + dx);
                        imageView.setTranslationY(imageView.getTranslationY() + dy);
                        lastX[0] = event.getX();
                        lastY[0] = event.getY();
                        break;
                }
            }
            return true;
        });

        android.widget.ImageButton btnClose = new android.widget.ImageButton(requireContext());
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClose.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        android.widget.FrameLayout.LayoutParams closeParams =
                new android.widget.FrameLayout.LayoutParams(120, 120);
        closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        closeParams.topMargin = 48;
        closeParams.rightMargin = 24;
        btnClose.setLayoutParams(closeParams);
        btnClose.setColorFilter(android.graphics.Color.WHITE);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        frameLayout.addView(imageView);
        frameLayout.addView(btnClose);

        dialog.setContentView(frameLayout);
        dialog.show();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}