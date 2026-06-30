package com.example.smartreceiptmanager.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.expense.ExpenseStore;
import com.example.smartreceiptmanager.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;

public class StatisticsFragment extends Fragment {

    private ExpenseStore expenseStore;
    private TextView tvFoodPercent;
    private TextView tvTransportPercent;
    private TextView tvShoppingPercent;
    private TextView tvOtherPercent;
    private BarChart barChartWeek;
    private PieChart pieChart;
    private TextView tabMonth;
    private TextView tab3Months;
    private TextView tabYear;

    //lưu các tab thống kê
    private static final int FILTER_MONTH = 0;
    private static final int FILTER_3_MONTHS = 1;
    private static final int FILTER_YEAR = 2;

    private int currentFilter = FILTER_MONTH;

    public StatisticsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expenseStore = new ExpenseStore(requireContext());

        tvFoodPercent = view.findViewById(R.id.tvFoodPercent);
        tvTransportPercent = view.findViewById(R.id.tvTransportPercent);
        tvShoppingPercent = view.findViewById(R.id.tvShoppingPercent);
        tvOtherPercent = view.findViewById(R.id.tvOtherPercent);

        barChartWeek = view.findViewById(R.id.barChartWeek);
        pieChart = view.findViewById(R.id.pieChart);

        //3 tab filter
        tabMonth = view.findViewById(R.id.tabMonth);
        tab3Months = view.findViewById(R.id.tab3Months);
        tabYear = view.findViewById(R.id.tabYear);

        //add clickListener
        tabMonth.setOnClickListener(v -> {
            currentFilter = FILTER_MONTH;
            refreshStatistics();
        });

        tab3Months.setOnClickListener(v -> {
            currentFilter = FILTER_3_MONTHS;
            refreshStatistics();
        });

        tabYear.setOnClickListener(v -> {
            currentFilter = FILTER_YEAR;
            refreshStatistics();
        });
        refreshStatistics();
    }

    private void refreshStatistics() {
        List<Expense> expenses = getFilteredExpenses();

        renderTopCategories(expenses);
        renderCategorySummary(expenses);
        renderPieChart(expenses);
        renderWeekChart(expenses);

        updateTabUI();
    }

    // lấy data theo tab filter
    private List<Expense> getFilteredExpenses() {
        switch (currentFilter) {
            case FILTER_3_MONTHS: {
                Calendar start = Calendar.getInstance();
                start.add(Calendar.MONTH, -2);
                start.set(Calendar.DAY_OF_MONTH, 1);

                return expenseStore.getExpensesBetween(start.getTimeInMillis(), System.currentTimeMillis());
            }
            case FILTER_YEAR: {
                Calendar start = Calendar.getInstance();
                start.set(Calendar.MONTH, Calendar.JANUARY);
                start.set(Calendar.DAY_OF_MONTH, 1);
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND,0);

                return expenseStore.getExpensesBetween(start.getTimeInMillis(), System.currentTimeMillis());
            }
            default:
                return expenseStore.getCurrentMonthExpenses();
        }
    }

    private void renderTopCategories(List<Expense> expenses) {
        LinearLayout container = requireView().findViewById(R.id.layoutTopCategoriesContainer);
        container.removeAllViews();

        if (expenses.isEmpty()) {
            return;
        }
        Map<String, Double> amountMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();

        double totalAmount = 0;
        for (Expense expense : expenses) {
            String category = expense.getCategory();
            double amount = expense.getAmount();
            totalAmount += amount;

            if (!amountMap.containsKey(category)) {
                amountMap.put(category, amount);
                countMap.put(category, 1);
            } else {
                amountMap.put(category, amountMap.get(category) + amount);
                countMap.put(category, countMap.get(category) + 1);
            }
        }
        if(totalAmount==0){
            return;
        }
        //sắp xếp Toptheo số tiền giảm dần
        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(amountMap.entrySet());
        Collections.sort(sortedList, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));

        for (Map.Entry<String, Double> entry : sortedList) {
            String category = entry.getKey();
            double amount = entry.getValue();
            int count = countMap.get(category);
            int progress = (int) (amount / totalAmount * 100);
            addTopCategory(container, getCategoryIcon(category), category, count + " giao dịch", CurrencyUtils.formatVnd(amount), progress);
        }
    }

    private String getCategoryIcon(String category) {
        switch (category) {
            case "Ăn uống":
                return "🍽️";
            case "Di chuyển":
                return "🚗";
            case "Mua sắm":
                return "🛍";
            case "Hóa đơn":
                return "🧾";
            default:
                return "💸";
        }
    }

    private void addTopCategory(LinearLayout container, String icon, String name, String count, String amount, int progress) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_top_category, container, false);

        TextView tvIcon = item.findViewById(R.id.tvCategoryIcon);
        TextView tvName = item.findViewById(R.id.tvCategoryName);
        TextView tvCount = item.findViewById(R.id.tvTransactionCount);
        TextView tvAmount = item.findViewById(R.id.tvCategoryAmount);
        ProgressBar progressBar = item.findViewById(R.id.pbIndicator);

        tvIcon.setText(icon);
        tvName.setText(name);
        tvCount.setText(count);
        tvAmount.setText(amount);
        progressBar.setProgress(progress);

        container.addView(item);
    }

    private void renderCategorySummary(List<Expense> expenses) {
        double total = 0;
        double food = 0;
        double transport = 0;
        double shopping = 0;
        double other = 0;

        for (Expense expense : expenses) {
            total += expense.getAmount();
            switch (expense.getCategory()) {
                case "Ăn uống":
                    food += expense.getAmount();
                    break;
                case "Di chuyển":
                    transport += expense.getAmount();
                    break;
                case "Mua sắm":
                    shopping += expense.getAmount();
                    break;
                default:
                    other += expense.getAmount();
            }
        }
//        tvTotalExpense.setText("Tổng\n" + CurrencyUtils.formatVnd(total));
        if (total == 0) {
            tvFoodPercent.setText("● Ăn uống 0%");
            tvTransportPercent.setText("● Di chuyển 0%");
            tvShoppingPercent.setText("● Mua sắm 0%");
            tvOtherPercent.setText("● Khác 0%");
            return;
        }

        tvFoodPercent.setText("● Ăn uống " + (int) (food * 100 / total) + "%");
        tvTransportPercent.setText("● Di chuyển " + (int) (transport * 100 / total) + "%");
        tvShoppingPercent.setText("● Mua sắm " + (int) (shopping * 100 / total) + "%");
        tvOtherPercent.setText("● Khác " + (int) (other * 100 / total) + "%");
    }

    private void renderWeekChart(List<Expense> expenses) {
        float[] total = new float[7];
        Calendar today = Calendar.getInstance();
        resetTime(today);
        for (Expense expense : expenses) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(expense.getDate());
            resetTime(c);

            long diffMillis = today.getTimeInMillis() - c.getTimeInMillis();
            int diff = (int) (diffMillis / (24 * 60 * 60 * 1000));

            if (diff >= 0 && diff < 7) {
                total[6 - diff] += expense.getAmount();
            }
        }

        ChartHelper.renderWeekChart(requireContext(), barChartWeek, total);
    }

    private void renderPieChart(List<Expense> expenses) {
        float food = 0;
        float transport = 0;
        float shopping = 0;
        float other = 0;
        float total = 0;

        for (Expense expense : expenses) {
            total += expense.getAmount();
            switch (expense.getCategory()) {
                case "Ăn uống":
                    food += expense.getAmount();
                    break;
                case "Di chuyển":
                    transport += expense.getAmount();
                    break;
                case "Mua sắm":
                    shopping += expense.getAmount();
                    break;
                default:
                    other += expense.getAmount();
            }
        }
        ChartHelper.renderPieChart(pieChart, requireContext(), food, transport, shopping, other, total);
    }

    // xử lý tránh lệch giờ
    private void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void updateTabUI() {

        // Reset tất cả tab
        tabMonth.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        tab3Months.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        tabYear.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        tabMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tab3Months.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tabYear.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        tabMonth.setTypeface(null, android.graphics.Typeface.NORMAL);
        tab3Months.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabYear.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Tab đang được chọn
        switch (currentFilter) {
            case FILTER_MONTH:
                tabMonth.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_surface));
                tabMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
                tabMonth.setTypeface(null, android.graphics.Typeface.BOLD);
                break;

            case FILTER_3_MONTHS:
                tab3Months.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_surface));
                tab3Months.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
                tab3Months.setTypeface(null, android.graphics.Typeface.BOLD);
                break;

            case FILTER_YEAR:
                tabYear.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_surface));
                tabYear.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
                tabYear.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
        }
    }

    //Statistic tự cập nhật sau khi thêm/sửa/xóa chi tiêu
    @Override
    public void onResume() {
        super.onResume();
        refreshStatistics();
    }
}