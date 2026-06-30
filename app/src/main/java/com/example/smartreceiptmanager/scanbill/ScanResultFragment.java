package com.example.smartreceiptmanager.scanbill;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.databinding.FragmentScanResultBinding;
import com.google.android.material.chip.Chip;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScanResultFragment extends Fragment {

    private static final String ARG_SHOP    = "shop_name";
    private static final String ARG_AMOUNT  = "amount";
    private static final String ARG_DATE    = "date";
    private static final String ARG_RAW     = "raw_text";
    private static final String ARG_CATEGORY = "category";
    private FragmentScanResultBinding binding;

    // Đường link URL lấy chính xác từ Console Realtime Database
    private final String dbUrl = "https://appmobile-123-default-rtdb.firebaseio.com/";

    public static Bitmap previewBitmap = null;

    public static ScanResultFragment newInstance(
            String shopName, long amount, long dateMillis, String rawText, String category) {
        Bundle args = new Bundle();
        args.putString(ARG_SHOP,   shopName);
        args.putLong  (ARG_AMOUNT, amount);
        args.putLong  (ARG_DATE,   dateMillis);
        args.putString(ARG_RAW,    rawText);
        args.putString(ARG_CATEGORY, category);
        ScanResultFragment f = new ScanResultFragment();
        f.setArguments(args);
        return f;
    }

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
        String category  = args.getString(ARG_CATEGORY, "Khác");

        if (previewBitmap != null) {
            binding.imgReceipt.setImageBitmap(previewBitmap);
        }

        binding.etShopName.setText(shopName);
        binding.etAmount.setText(String.valueOf(amount));
        binding.etDate.setText(
                new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        .format(new Date(dateMs)));

        binding.tvConfidence.setText("AI gợi ý danh mục: " + category);

        // KIỂM TRA KẾT NỐI MẠNG ĐƠN LẺ
        com.google.firebase.database.FirebaseDatabase.getInstance(dbUrl)
                .getReference(".info/connected")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        Boolean connected = snapshot.getValue(Boolean.class);
                        if (isAdded()) {
                            if (connected != null && connected) {
                                Toast.makeText(requireContext(), "✅ Kết nối Realtime DB thành công!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "⚠️ Firebase đang ngoại tuyến hoặc sai Rules!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });

        // Hiện bộ chip tĩnh cục bộ ngay lập tức để giao diện mượt mà
        renderLocalPredictionChips(category);

        //TẢI DANH MỤC TRỰC TUYẾN TỪ SERVER
        loadCategoriesFromRealtime(category);

        binding.btnViewLarge.setOnClickListener(v -> {
            if (previewBitmap == null) return;
            showFullImageDialog();
        });

        binding.btnRescan.setOnClickListener(v -> {
            previewBitmap = null;
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        //LUỒNG TỰ ĐỘNG MÃ HÓA ẢNH THÀNH URL BASE64 VÀ GHI VÀO DATABASE
        binding.btnSave.setOnClickListener(v -> {
            binding.btnSave.setText("Đang mã hóa & lưu...");
            binding.btnSave.setEnabled(false);

            String finalShop   = binding.etShopName.getText() != null ? binding.etShopName.getText().toString() : shopName;
            String finalAmount = binding.etAmount.getText() != null ? binding.etAmount.getText().toString() : "0";
            String finalDate   = binding.etDate.getText() != null ? binding.etDate.getText().toString() : "";

            int categoryId = 8; // Mặc định là Khác
            int checkedId = binding.chipGroup.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                Chip selectedChip = binding.chipGroup.findViewById(checkedId);
                if (selectedChip != null) {
                    if (selectedChip.getTag() != null) {
                        categoryId = (int) selectedChip.getTag();
                    } else {
                        categoryId = getCategoryId(selectedChip.getText().toString());
                    }
                }
            }

            // Fix lỗi định dạng số tiền nếu chứa ký tự đặc biệt
            String cleanAmountStr = finalAmount.replaceAll("[^0-9]", "");
            long amountLong = 0;
            try {
                if (!cleanAmountStr.isEmpty()) {
                    amountLong = Long.parseLong(cleanAmountStr);
                }
            } catch (NumberFormatException ignored) {}

            String imageConvertedUrl = "";

            if (previewBitmap != null) {
                try {
                    //  Thu nhỏ kích thước ảnh một chút để chuỗi URL không bị quá nặng cho Database
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(previewBitmap, 600, 800, true);

                    //  Nén ảnh Bitmap thành mảng Byte dạng JPEG chất lượng 70%
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] imageData = baos.toByteArray();

                    //  Chuyển mảng Byte thành chuỗi ký tự dài Base64 (Đóng vai trò làm URL nội dung ảnh)
                    imageConvertedUrl = Base64.encodeToString(imageData, Base64.DEFAULT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Ghi trực tiếp chuỗi URL ảnh đã chuyển đổi và text về Realtime Database (Bỏ hoàn toàn qua Storage)
            luuHoaDonVaoRealtime(finalShop, amountLong, finalDate, categoryId, rawText, imageConvertedUrl);
        });
    }

    private void resetSaveButtonState() {
        if (binding != null) {
            binding.btnSave.setText("Lưu hóa đơn");
            binding.btnSave.setEnabled(true);
        }
    }

    // HÀM GHI DỮ LIỆU CUỐI CÙNG LÊN REALTIME DATABASE
    private void luuHoaDonVaoRealtime(String shop, long amount, String date, int catId, String raw, String imageUrl) {

        com.google.firebase.auth.FirebaseUser currentUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            resetSaveButtonState();
            if (isAdded()) {
                Toast.makeText(requireContext(), "⚠️ Bạn cần đăng nhập để lưu hóa đơn!", Toast.LENGTH_SHORT).show();
            }
            return; // dừng lại, không ghi DB nếu chưa đăng nhập
        }
        String uid = currentUser.getUid();

        // Tính next_due_date = ngày hóa đơn + 1 tháng (mặc định Monthly)
        // dành cho hóa đơn tiền điên, tiền thue nhà
        String nextDueDate = date;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            Date parsedDate = sdf.parse(date);
            if (parsedDate != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(parsedDate);
                cal.add(java.util.Calendar.MONTH, 1);
                nextDueDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        Map<String, Object> recurringTx = new HashMap<>();
        recurringTx.put("amount",             amount);
        recurringTx.put("category_id",        String.valueOf(catId));
        recurringTx.put("frequency",          "Monthly");
        recurringTx.put("next_due_date",      nextDueDate);
        recurringTx.put("wallet_id",          "wallet_default_01");
        recurringTx.put("shop_name",          shop);
        recurringTx.put("note",               raw);
        recurringTx.put("receipt_image_url",  imageUrl);
        recurringTx.put("created_at",         System.currentTimeMillis());

        // Ghi đúng theo cấu trúc: User_Profiles/recurring_transactions/{uid}/{id}
        com.google.firebase.database.DatabaseReference recurringRef = com.google.firebase.database.FirebaseDatabase
                .getInstance(dbUrl)
                .getReference("User_Profiles")
                .child("recurring_transactions")
                .child(uid);

        String key = recurringRef.push().getKey();
        if (key != null) {
            recurringRef.child(key).setValue(recurringTx)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            resetSaveButtonState();
                            Toast.makeText(requireContext(), "🎉 Đã lưu hóa đơn thành công!", Toast.LENGTH_SHORT).show();
                            previewBitmap = null;
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            resetSaveButtonState();
                            Toast.makeText(requireContext(), "❌ Lỗi ghi Database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void renderLocalPredictionChips(String aiSuggestedCategory) {
        if (binding == null) return;

        binding.chipGroup.removeAllViews();
        binding.progressCategories.setVisibility(View.VISIBLE);
        binding.chipGroup.setVisibility(View.VISIBLE);

        String[] defaultCategories = {"Ăn uống", "Cà phê", "Mua sắm", "Di chuyển", "Giải trí", "Sức khỏe", "Giáo dục", "Khác"};

        for (String catName : defaultCategories) {
            Chip chip = new Chip(requireContext());
            chip.setText(catName);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setTag(getCategoryId(catName));

            if (catName.equalsIgnoreCase(aiSuggestedCategory)) {
                chip.setChecked(true);
            }
            binding.chipGroup.addView(chip);
        }

        if (binding.chipGroup.getCheckedChipId() == View.NO_ID) {
            for (int i = 0; i < binding.chipGroup.getChildCount(); i++) {
                View child = binding.chipGroup.getChildAt(i);
                if (child instanceof Chip && "Khác".equals(((Chip) child).getText().toString())) {
                    ((Chip) child).setChecked(true);
                    break;
                }
            }
        }
    }

    private void loadCategoriesFromRealtime(String aiSuggestedCategory) {
        com.google.firebase.database.FirebaseDatabase.getInstance(dbUrl).getReference("Categories")
                .orderByChild("category_type")
                .equalTo("expense")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (binding == null) return;
                    binding.progressCategories.setVisibility(View.GONE);

                    if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                        binding.chipGroup.removeAllViews();

                        for (com.google.firebase.database.DataSnapshot doc : dataSnapshot.getChildren()) {
                            String name = doc.child("name").getValue(String.class);
                            Long categoryIdLong = doc.child("category_id").getValue(Long.class);
                            int categoryId = categoryIdLong != null ? categoryIdLong.intValue() : 0;

                            Chip chip = new Chip(requireContext());
                            chip.setText(name);
                            chip.setTag(categoryId);
                            chip.setCheckable(true);
                            chip.setCheckedIconVisible(true);

                            if (name != null && name.equalsIgnoreCase(aiSuggestedCategory)) {
                                chip.setChecked(true);
                            }
                            binding.chipGroup.addView(chip);
                        }

                        if (binding.chipGroup.getCheckedChipId() == View.NO_ID) {
                            for (int i = 0; i < binding.chipGroup.getChildCount(); i++) {
                                View child = binding.chipGroup.getChildAt(i);
                                if (child instanceof Chip && "Khác".equals(((Chip) child).getText().toString())) {
                                    ((Chip) child).setChecked(true);
                                    break;
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        binding.progressCategories.setVisibility(View.GONE);
                    }
                });
    }

    private int getCategoryId(String categoryName) {
        switch (categoryName) {
            case "Ăn uống":   return 1;
            case "Cà phê":    return 2;
            case "Mua sắm":   return 3;
            case "Di chuyển": return 4;
            case "Giải trí":  return 5;
            case "Sức khỏe":  return 6;
            case "Giáo dục":  return 7;
            default:          return 8;
        }
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