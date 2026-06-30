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
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {
    private ExpenseStore expenseStore;
    private TextView txtBalance;
    private TextView txtWeekTotal;
    private TextView txtEmptyExpense;
    private LinearLayout layoutExpenseList;
    private LinearLayout layoutWeekChart;
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
        layoutWeekChart = view.findViewById(R.id.layoutWeekChart);

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

        txtBalance.setText(CurrencyUtils.formatVnd(savedExpenses.isEmpty() ? 12450000 : expenseStore.getCurrentMonthTotal()));
        txtWeekTotal.setText(CurrencyUtils.formatVnd(getWeekTotal(expenses)));
        txtEmptyExpense.setVisibility(View.GONE);
        renderWeekChart(expenses);

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

    private double getWeekTotal(List<Expense> expenses) {
        long anchorDate = expenses.isEmpty() ? System.currentTimeMillis() : expenses.get(0).getDate();
        double total = 0;
        for (Expense expense : expenses) {
            if (isSameWeek(anchorDate, expense.getDate())) {
                total += expense.getAmount();
            }
        }
        return total;
    }

    private void renderWeekChart(List<Expense> expenses) {
        if (layoutWeekChart == null) return;

        layoutWeekChart.removeAllViews();
        String[] labels = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        long anchorDate = expenses.isEmpty() ? System.currentTimeMillis() : expenses.get(0).getDate();
        int activeIndex = getWeekIndex(anchorDate);
        double[] totals = new double[7];
        double maxTotal = 0;

        for (Expense expense : expenses) {
            if (isSameWeek(anchorDate, expense.getDate())) {
                int index = getWeekIndex(expense.getDate());
                totals[index] += expense.getAmount();
                maxTotal = Math.max(maxTotal, totals[index]);
            }
        }

        for (int index = 0; index < labels.length; index++) {
            boolean isActive = index == activeIndex;
            LinearLayout column = new LinearLayout(requireContext());
            column.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
            column.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            column.setOrientation(LinearLayout.VERTICAL);
            if (index < labels.length - 1) {
                column.setPadding(0, 0, dp(2), 0);
            }

            TextView label = new TextView(requireContext());
            label.setText(isActive ? labels[index] : "");
            label.setTextColor(getResources().getColor(R.color.primary_green));
            label.setTextSize(13);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            label.setGravity(android.view.Gravity.CENTER);
            column.addView(label, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(20)
            ));

            View bar = new View(requireContext());
            int barHeight = getBarHeight(totals[index], maxTotal, isActive, index, activeIndex);
            bar.setBackgroundColor(getResources().getColor(getChartBarColor(totals[index], isActive, index, activeIndex)));
            column.addView(bar, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(barHeight)
            ));

            layoutWeekChart.addView(column);
        }
    }

    private int getBarHeight(double amount, double maxTotal, boolean isActive, int index, int activeIndex) {
        if (isActive) {
            return 58;
        }
        if (index == activeIndex - 1) {
            return 34;
        }
        if (index == activeIndex - 2) {
            return 24;
        }
        if (index == activeIndex + 1) {
            return 20;
        }
        if (maxTotal <= 0) {
            return 8;
        }
        if (amount <= 0) {
            return 8;
        }
        int minHeight = 18;
        int maxHeight = 42;
        return minHeight + (int) Math.round((amount / maxTotal) * (maxHeight - minHeight));
    }

    private int getChartBarColor(double amount, boolean isActive, int index, int activeIndex) {
        if (isActive) {
            return R.color.primary_green;
        }
        return amount > 0 || Math.abs(index - activeIndex) <= 2 ? R.color.chart_green_soft : R.color.chart_gray_soft;
    }

    private boolean isSameWeek(long firstDate, long secondDate) {
        Calendar first = Calendar.getInstance();
        Calendar second = Calendar.getInstance();
        first.setFirstDayOfWeek(Calendar.MONDAY);
        second.setFirstDayOfWeek(Calendar.MONDAY);
        first.setTimeInMillis(firstDate);
        second.setTimeInMillis(secondDate);
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.WEEK_OF_YEAR) == second.get(Calendar.WEEK_OF_YEAR);
    }

    private int getWeekIndex(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        return (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
    @Override
    public void onResume() {
        super.onResume();
        renderExpenses();
    }
}
