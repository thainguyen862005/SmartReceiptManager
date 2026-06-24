package com.example.smartreceiptmanager.home;

import android.content.Intent;
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
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.smartreceiptmanager.ProfileActivity;
import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.auth.AuthViewModel;
import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.expense.ExpenseDetailFragment;
import com.example.smartreceiptmanager.expense.ExpenseStore;
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.example.smartreceiptmanager.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private ExpenseStore expenseStore;
    private TextView txtBalance;
    private TextView txtWeekTotal;
    private TextView txtEmptyExpense;
    private LinearLayout layoutExpenseList;
    private AuthViewModel authViewModel;
    private ImageView imgHeaderAvatar;
    private View cardHeaderAvatar;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);
        cardHeaderAvatar = view.findViewById(R.id.cardHeaderAvatar);
        imgHeaderAvatar = view.findViewById(R.id.imgHeaderAvatar);

        // 2. Khởi tạo AuthViewModel (Sử dụng requireActivity() để dùng chung tầng dữ liệu với Activity)
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // 3. Lắng nghe thông tin User để tự động load ảnh đại diện thực tế
        authViewModel.getUserLiveData().observe(getViewLifecycleOwner(), firebaseUser -> {
            if (firebaseUser != null && firebaseUser.getPhotoUrl() != null) {
                if (imgHeaderAvatar != null) {
                    Glide.with(this)
                            .load(firebaseUser.getPhotoUrl())
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .circleCrop()
                            .into(imgHeaderAvatar);
                }
            }
        });

        // 4. Bắt sự kiện click vào ô tròn Avatar để mở màn hình Thông tin cá nhân (Profile)
        if (cardHeaderAvatar != null) {
            cardHeaderAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                startActivity(intent);
            });
        }

        expenseStore = new ExpenseStore(requireContext());
        txtBalance = view.findViewById(R.id.txtBalance);
        txtWeekTotal = view.findViewById(R.id.txtWeekTotal);
        txtEmptyExpense = view.findViewById(R.id.txtEmptyExpense);
        layoutExpenseList = view.findViewById(R.id.layoutExpenseList);

        // ĐÃ XÓA KHỐI CODE btnAddExpense TẠI ĐÂY ĐỂ TRÁNH LỖI CRASH ỨNG DỤNG

        // Nút "Xem tất cả" vẫn giữ lại hoạt động bình thường để nhảy sang tab Lịch sử
        view.findViewById(R.id.btnViewAll).setOnClickListener(v -> {
            requireActivity().findViewById(R.id.btnHistory).performClick();
        });

        renderExpenses();
    }

    private void renderExpenses() {
        List<Expense> savedExpenses = expenseStore.getAllExpenses();
        List<Expense> expenses = savedExpenses.isEmpty() ? getPreviewExpenses() : savedExpenses;

        txtBalance.setText("12,450,000 đ");
        txtWeekTotal.setText(CurrencyUtils.formatVnd(savedExpenses.isEmpty() ? 3200000 : expenseStore.getCurrentMonthTotal()));
        txtEmptyExpense.setVisibility(View.GONE);

        layoutExpenseList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        int count = Math.min(expenses.size(), 3);
        for (int i = 0; i < count; i++) {
            Expense expense = expenses.get(i);
            View itemView = inflater.inflate(R.layout.item_expense, layoutExpenseList, false);

            TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
            TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
            TextView txtMeta = itemView.findViewById(R.id.txtMeta);
            TextView txtAmount = itemView.findViewById(R.id.txtAmount);
            TextView btnDelete = itemView.findViewById(R.id.btnDeleteExpense);

            txtIcon.setText(getCategoryIcon(expense.getCategory()));
            txtIcon.setBackgroundResource(getCategoryBackground(expense.getCategory()));
            txtMerchant.setText(expense.getMerchantName());
            txtMeta.setText(getFriendlyDate(expense));
            txtAmount.setText("-" + CurrencyUtils.formatVnd(expense.getAmount()));

            itemView.setOnClickListener(v -> openExpenseDetail(expense.getId()));
            btnDelete.setOnClickListener(v -> {
                expenseStore.deleteExpense(expense.getId());
                Toast.makeText(requireContext(), "Đã xóa khoản chi", Toast.LENGTH_SHORT).show();
                renderExpenses();
            });

            layoutExpenseList.addView(itemView);
        }
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

    private List<Expense> getPreviewExpenses() {
        long now = System.currentTimeMillis();
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("preview-1", "Ăn tối ngoài", 450000, "Ăn uống", now, "Hôm nay, 19:30", "", false, now, now));
        expenses.add(new Expense("preview-2", "Grab Bike", 45000, "Di chuyển", now, "Hôm nay, 08:15", "", false, now, now));
        expenses.add(new Expense("preview-3", "Siêu thị Mini", 120000, "Mua sắm", now - 86400000, "Hôm qua, 18:00", "", false, now, now));
        return expenses;
    }

    private String getFriendlyDate(Expense expense) {
        if (expense.getNote() != null && expense.getNote().startsWith("Hôm")) {
            return expense.getNote();
        }

        return DateUtils.formatDate(expense.getDate());
    }

    private String getCategoryIcon(String category) {
        if ("Ăn uống".equals(category)) {
            return "🍴";
        } else if ("Di chuyển".equals(category)) {
            return "🚗";
        } else if ("Mua sắm".equals(category)) {
            return "🛍";
        } else if ("Hóa đơn".equals(category)) {
            return "🧾";
        }

        return "💸";
    }

    private int getCategoryBackground(String category) {
        if ("Ăn uống".equals(category)) {
            return R.drawable.bg_avatar_mint;
        } else if ("Di chuyển".equals(category)) {
            return R.drawable.bg_avatar_blue;
        } else if ("Mua sắm".equals(category)) {
            return R.drawable.bg_avatar_gray;
        } else if ("Hóa đơn".equals(category)) {
            return R.drawable.bg_avatar_gray;
        }

        return R.drawable.bg_avatar_gray;
    }
}