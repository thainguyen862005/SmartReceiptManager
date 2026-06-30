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
import androidx.lifecycle.ViewModelProvider;
import com.example.smartreceiptmanager.auth.AuthViewModel;

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

public class ExpenseListFragment extends Fragment {

    private TextView txtEmpty;
    private LinearLayout layoutAllExpenses;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtEmpty = view.findViewById(R.id.txtEmptyExpenseList);
        layoutAllExpenses = view.findViewById(R.id.layoutAllExpenses);

        // Khởi động việc lắng nghe dữ liệu từ Firebase
        loadExpensesFromFirebase();
    }

    private void loadExpensesFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (imgHeaderAvatar != null) {
            AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
            authViewModel.getUserProfileLiveData().observe(getViewLifecycleOwner(), userProfile -> {
                if (userProfile != null && userProfile.getProfile() != null) {
                    String avatarUrl = userProfile.getProfile().getAvatar_url();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        if (avatarUrl.startsWith("http")) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(android.R.drawable.sym_def_app_icon)
                                    .circleCrop()
                                    .into(imgHeaderAvatar);
                        } else {
                            try {
                                byte[] bytes = android.util.Base64.decode(avatarUrl, android.util.Base64.DEFAULT);
                                Glide.with(this)
                                        .load(bytes)
                                        .placeholder(android.R.drawable.sym_def_app_icon)
                                        .circleCrop()
                                        .into(imgHeaderAvatar);
                            } catch (Exception ignored) {
                            }
                        }

        String uid = currentUser.getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("User_Profiles")
                .child("transactions")
                .child(uid);

        // Lắng nghe dữ liệu thời gian thực
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (layoutAllExpenses == null) return;

                List<Expense> expenses = new ArrayList<>();

                // Đọc dữ liệu từ Firebase
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Expense expense = new Expense();
                        expense.setId(data.getKey());

                        Double amount = data.child("amount").getValue(Double.class);
                        expense.setAmount(amount != null ? amount : 0);

                        String note = data.child("note").getValue(String.class);
                        expense.setNote(note != null ? note : "");

                        // Lấy danh mục
                        DataSnapshot categorySnap = data.child("category");
                        String categoryName = "Khác";
                        if (categorySnap.exists()) {
                            String name = categorySnap.child("name").getValue(String.class);
                            if (name != null) categoryName = name;
                        }
                        expense.setCategory(categoryName);

                        // Tên hiển thị chính (Merchant)
                        if (note != null && !note.trim().isEmpty()) {
                            expense.setMerchantName(note);
                        } else {
                            expense.setMerchantName(categoryName);
                        }

                        // Phân loại Thu/Chi dựa vào type
                        String type = data.child("type").getValue(String.class);
                        if ("income".equals(type)) {
                            expense.setCategory("Thu nhập"); // Gắn tạm để UI xử lý màu xanh
                        }

                        // Lấy thời gian
                        String dateStr = data.child("transaction_date").getValue(String.class);
                        expense.setDate(parseDateStringToLong(dateStr));

                        expenses.add(expense);
                    }
                }

                // Sắp xếp danh sách mới nhất lên đầu
                Collections.sort(expenses, (e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));

                // Gọi hàm hiển thị ra UI
                renderExpenses(expenses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi tải dữ liệu: " + error.getMessage());
            }
        });
    }

    private void renderExpenses(List<Expense> expenses) {
        txtEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
        layoutAllExpenses.removeAllViews();

        if (expenses.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String currentGroup = "";

        for (int index = 0; index < expenses.size(); index++) {
            Expense expense = expenses.get(index);

            // Logic tạo Header (Nhóm theo ngày)
            String group = DateUtils.formatDate(expense.getDate());
            if (!group.equals(currentGroup)) {
                currentGroup = group;
                layoutAllExpenses.addView(createGroupHeader(group));
            }

            // Nạp View cho 1 item chi tiêu
            View itemView = inflater.inflate(R.layout.item_expense, layoutAllExpenses, false);
            TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
            TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
            TextView txtMeta = itemView.findViewById(R.id.txtMeta);
            TextView txtAmount = itemView.findViewById(R.id.txtAmount);
            TextView btnDelete = itemView.findViewById(R.id.btnDeleteExpense);

            txtIcon.setText(getCategoryIcon(expense.getCategory()));
            txtIcon.setBackgroundResource(getCategoryBackground(expense.getCategory()));

            txtMerchant.setText(expense.getMerchantName());

            // Thay thế hàm getTimeText cũ (hardcode) bằng thời gian thật
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String realTime = timeFormat.format(new Date(expense.getDate()));
            txtMeta.setText(realTime + " • " + expense.getCategory());

            // Xử lý màu sắc Tiền Thu / Tiền Chi
            if ("Thu nhập".equals(expense.getCategory())) {
                txtAmount.setText("+ " + CurrencyUtils.formatVnd(expense.getAmount()));
                txtAmount.setTextColor(getResources().getColor(R.color.primary_green)); // Đảm bảo bạn có màu này trong colors.xml
            } else {
                txtAmount.setText("- " + CurrencyUtils.formatVnd(expense.getAmount()));
                txtAmount.setTextColor(0xFFB9181E);
            }

            // Sự kiện Click để mở màn hình chi tiết
            itemView.setOnClickListener(v -> openExpenseDetail(expense.getId()));
            if(btnDelete != null) {
                btnDelete.setOnClickListener(v -> openExpenseDetail(expense.getId()));
            }

            layoutAllExpenses.addView(itemView);
        }
    }

    private TextView createGroupHeader(String title) {
        TextView header = new TextView(requireContext());
        header.setText(title);
        header.setTextColor(0xFF435047);
        header.setTextSize(18); // Chỉnh lại size một chút cho hài hòa
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 32, 0, 16);
        return header;
    }

    private void openExpenseDetail(String expenseId) {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, ExpenseDetailFragment.newInstance(expenseId))
                .addToBackStack(null)
                .commit();

        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
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