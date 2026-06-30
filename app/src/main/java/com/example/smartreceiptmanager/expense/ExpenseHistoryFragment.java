package com.example.smartreceiptmanager.expense;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseHistoryFragment extends Fragment {

    private LinearLayout layoutAllExpenses;
    private TextView txtEmptyExpenseList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Nạp đúng file XML chứa layoutAllExpenses và txtEmptyExpenseList
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutAllExpenses = view.findViewById(R.id.layoutAllExpenses);
        txtEmptyExpenseList = view.findViewById(R.id.txtEmptyExpenseList);

        loadExpensesFromFirebase();
    }

    private void loadExpensesFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem lịch sử", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        // Kết nối đến nhánh dữ liệu transactions của người dùng trên Firebase
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("User_Profiles")
                .child("transactions")
                .child(uid);

        // Lắng nghe dữ liệu thời gian thực
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (layoutAllExpenses == null) return;

                layoutAllExpenses.removeAllViews(); // Xóa sạch dữ liệu cũ trước khi nạp mới
                List<Expense> expenseList = new ArrayList<>();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    txtEmptyExpenseList.setVisibility(View.VISIBLE);
                    layoutAllExpenses.setVisibility(View.GONE);
                    return;
                }

                txtEmptyExpenseList.setVisibility(View.GONE);
                layoutAllExpenses.setVisibility(View.VISIBLE);

                // Duyệt và bóc tách dữ liệu từ Firebase đổ vào Model
                for (DataSnapshot data : snapshot.getChildren()) {
                    Expense expense = new Expense();
                    expense.setId(data.getKey());

                    Double amount = data.child("amount").getValue(Double.class);
                    expense.setAmount(amount != null ? amount : 0);

                    String note = data.child("note").getValue(String.class);
                    expense.setNote(note != null ? note : "");

                    // Lấy thông tin danh mục (Category) từ object category -> name
                    DataSnapshot categorySnap = data.child("category");
                    String categoryName = "Khác";
                    if (categorySnap.exists()) {
                        String name = categorySnap.child("name").getValue(String.class);
                        if (name != null) categoryName = name;
                    }
                    expense.setCategory(categoryName);

                    // Thiết lập tên hiển thị chính (Merchant) giống logic XML bạn muốn hiển thị
                    if (note != null && !note.trim().isEmpty()) {
                        expense.setMerchantName(note);
                    } else {
                        expense.setMerchantName(categoryName);
                    }

                    // Nhận diện giao dịch là Thu nhập (income) hay Chi tiêu (expense)
                    String type = data.child("type").getValue(String.class);
                    if ("income".equals(type)) {
                        expense.setCategory("Thu nhập"); // Gán nhãn để xử lý màu sắc UI bên dưới
                    }

                    String dateStr = data.child("transaction_date").getValue(String.class);
                    expense.setDate(parseDateStringToLong(dateStr));

                    expenseList.add(expense);
                }

                // Sắp xếp các giao dịch giảm dần theo thời gian (mới nhất hiển thị lên đầu)
                Collections.sort(expenseList, (e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));

                // Đổ dữ liệu ra giao diện kèm theo tính năng tự động tạo Group Header (Nhóm theo ngày)
                String currentGroup = "";
                LayoutInflater inflater = LayoutInflater.from(requireContext());

                for (int i = 0; i < expenseList.size(); i++) {
                    Expense exp = expenseList.get(i);

                    // Lấy chuỗi ngày tháng (Ví dụ: "Hôm nay", "24/10/2023"...) từ hàm Utils của bạn
                    String group = DateUtils.formatDate(exp.getDate());
                    if (!group.equals(currentGroup)) {
                        currentGroup = group;
                        layoutAllExpenses.addView(createGroupHeader(group)); // Thêm dòng tiêu đề ngày
                    }

                    addExpenseViewToLayout(exp, inflater);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi tải lịch sử: " + error.getMessage());
            }
        });
    }

    // Hàm tạo TextView làm tiêu đề nhóm ngày tháng trực quan
    private TextView createGroupHeader(String title) {
        TextView header = new TextView(requireContext());
        header.setText(title);
        header.setTextColor(0xFF435047);
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 24, 0, 12);
        return header;
    }

    private void addExpenseViewToLayout(Expense expense, LayoutInflater inflater) {
        // Nạp chính xác layout con item_expense.xml của bạn
        View itemView = inflater.inflate(R.layout.item_expense, layoutAllExpenses, false);

        // Khởi tạo và khớp chuẩn 100% ID từ file item_expense.xml của bạn
        TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
        TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
        TextView txtMeta = itemView.findViewById(R.id.txtMeta);
        TextView txtAmount = itemView.findViewById(R.id.txtAmount);

        // 1. Gán Tên hiển thị (Merchant)
        txtMerchant.setText(expense.getMerchantName());

        // 2. Gán Emoji danh mục và màu nền Avatar hình tròn tương ứng
        txtIcon.setText(getCategoryIcon(expense.getCategory()));
        txtIcon.setBackgroundResource(getCategoryBackground(expense.getCategory()));

        // 3. Xử lý thời gian thực tế dạng Giờ:Phút (HH:mm) kết hợp tên Danh mục
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String realTime = timeFormat.format(new Date(expense.getDate()));
        txtMeta.setText(realTime + " • " + expense.getCategory());

        // 4. Xử lý định dạng tiền và đổi màu linh hoạt dựa trên tính chất giao dịch
        if ("Thu nhập".equals(expense.getCategory())) {
            txtAmount.setText("+ " + CurrencyUtils.formatVnd(expense.getAmount()));
            txtAmount.setTextColor(0xFF006F49); // Màu xanh lục Mint thương hiệu của bạn
        } else {
            txtAmount.setText("- " + CurrencyUtils.formatVnd(expense.getAmount()));
            txtAmount.setTextColor(0xFFB9181E); // Màu đỏ chi tiêu nguy hiểm
        }

        // 5. Sự kiện click vào item mở màn hình Chi tiết chi tiêu (ExpenseDetailFragment)
        itemView.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, ExpenseDetailFragment.newInstance(expense.getId()))
                    .addToBackStack(null)
                    .commit();

            // Ẩn Bottom Navigation Bar khi vào xem chi tiết để tối ưu không gian hiển thị
            View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        });

        layoutAllExpenses.addView(itemView);
    }

    private String getCategoryIcon(String category) {
        if ("Ăn uống".equals(category)) return "🍴";
        if ("Di chuyển".equals(category)) return "🚗";
        if ("Mua sắm".equals(category)) return "🛍";
        if ("Hóa đơn".equals(category)) return "🧾";
        if ("Thu nhập".equals(category)) return "💵";
        if ("Sinh hoạt".equals(category)) return "🛍";
        return "💸";
    }

    private int getCategoryBackground(String category) {
        if ("Ăn uống".equals(category) || "Thu nhập".equals(category)) {
            return R.drawable.bg_avatar_green;
        } else if ("Di chuyển".equals(category)) {
            return R.drawable.bg_avatar_blue;
        } else if ("Sinh hoạt".equals(category)) {
            return R.drawable.bg_avatar_mint;
        }
        return R.drawable.bg_avatar_gray;
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