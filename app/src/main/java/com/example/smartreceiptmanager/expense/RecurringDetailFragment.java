package com.example.smartreceiptmanager.expense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.example.smartreceiptmanager.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecurringDetailFragment extends Fragment {
    private static final String ARG_EXPENSE_ID = "recurring_expense_id";
    private String currentExpenseId;

    private TextView txtCategoryPill;
    private TextView txtDetailAmount;
    private TextView txtDetailDate;
    private TextView txtShopName;
    private TextView txtDetailNote;

    // Khai báo UI cho hình ảnh
    private LinearLayout layoutReceiptImage;
    private ImageView imgReceipt;

    public static RecurringDetailFragment newInstance(String expenseId) {
        RecurringDetailFragment fragment = new RecurringDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXPENSE_ID, expenseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recurring_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentExpenseId = getArguments() == null ? null : getArguments().getString(ARG_EXPENSE_ID);

        txtCategoryPill = view.findViewById(R.id.txtCategoryPill);
        txtDetailAmount = view.findViewById(R.id.txtDetailAmount);
        txtDetailDate = view.findViewById(R.id.txtDetailDate);
        txtShopName = view.findViewById(R.id.txtShopName);
        txtDetailNote = view.findViewById(R.id.txtDetailNote);

        // Ánh xạ UI hình ảnh
        layoutReceiptImage = view.findViewById(R.id.layoutReceiptImage);
        imgReceipt = view.findViewById(R.id.imgReceipt);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> close());
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> deleteRecurringExpense());

        if (currentExpenseId != null) {
            loadRecurringDataFromFirebase();
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy mã giao dịch", Toast.LENGTH_SHORT).show();
            close();
        }
    }

    private void loadRecurringDataFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("User_Profiles")
                .child("recurring_transactions")
                .child(currentUser.getUid())
                .child(currentExpenseId);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot data) {
                if (!isAdded()) return;

                if (data.exists()) {
                    // 1. Lấy Số tiền
                    double amount = 0;
                    Object amtObj = data.child("amount").getValue();
                    if (amtObj instanceof Number) amount = ((Number) amtObj).doubleValue();
                    else if (amtObj instanceof String) {
                        try { amount = Double.parseDouble((String) amtObj); } catch (Exception ignored) {}
                    }
                    txtDetailAmount.setText("- " + CurrencyUtils.formatVnd(amount));

                    // 2. Lấy Tên cửa hàng / Chu kỳ
                    String shop = data.child("shop_name").getValue(String.class);
                    String freq = data.child("frequency").getValue(String.class);
                    if (shop != null && !shop.trim().isEmpty()) {
                        txtShopName.setText("Cửa hàng: " + shop);
                        txtCategoryPill.setText("🧾 Hóa đơn quét");
                    } else if (freq != null && !freq.trim().isEmpty()) {
                        txtShopName.setText("Chu kỳ: " + freq);
                        txtCategoryPill.setText("🔄 Định kỳ");
                    } else {
                        txtShopName.setText("Không xác định");
                        txtCategoryPill.setText("🧾 Khác");
                    }

                    // 3. Lấy Ghi chú
                    String note = data.child("note").getValue(String.class);
                    txtDetailNote.setText(note != null && !note.isEmpty() ? note : "Không có ghi chú");

                    // 4. Lấy Ngày giờ tạo
                    long dateLong = System.currentTimeMillis();
                    Object createdAtObj = data.child("created_at").getValue();
                    if (createdAtObj instanceof Number) {
                        dateLong = ((Number) createdAtObj).longValue();
                    } else if (createdAtObj instanceof String) {
                        dateLong = parseDateStringToLong((String) createdAtObj);
                    }
                    txtDetailDate.setText("▦ " + DateUtils.formatDateTime(dateLong));

                    // ==========================================
                    // 5. LOAD HÌNH ẢNH TỪ CHUỖI BASE64
                    // ==========================================
                    String base64Image = data.child("receipt_image_url").getValue(String.class);

                    if (base64Image != null && !base64Image.trim().isEmpty()) {
                        layoutReceiptImage.setVisibility(View.VISIBLE);
                        try {
                            // Cắt bỏ phần đầu (nếu có, ví dụ data:image/jpeg;base64,)
                            if (base64Image.contains(",")) {
                                base64Image = base64Image.split(",")[1];
                            }
                            // Giải mã chuỗi Base64 thành mảng byte
                            byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            // Chuyển mảng byte thành Bitmap
                            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            // Set ảnh vào ImageView
                            imgReceipt.setImageBitmap(decodedByte);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Nếu giải mã lỗi, hiển thị icon báo lỗi
                            imgReceipt.setImageResource(android.R.drawable.ic_dialog_alert);
                        }
                    } else {
                        // Nếu không có ảnh thì ẩn layout đi
                        layoutReceiptImage.setVisibility(View.GONE);
                    }

                } else {
                    Toast.makeText(requireContext(), "Dữ liệu không tồn tại", Toast.LENGTH_SHORT).show();
                    close();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteRecurringExpense() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentExpenseId == null) return;

        FirebaseDatabase.getInstance().getReference("User_Profiles")
                .child("recurring_transactions")
                .child(currentUser.getUid())
                .child(currentExpenseId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Đã xóa thành công", Toast.LENGTH_SHORT).show();
                    close();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void close() {
        requireActivity().getSupportFragmentManager().popBackStack();
        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
    }

    private long parseDateStringToLong(String dateStr) {
        if (dateStr == null) return System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date date = sdf.parse(dateStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }
}