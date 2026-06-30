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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

public class StatisticsFragment extends Fragment {

    private ExpenseStore expenseStore;
    private TextView tvTotalExpense;
    private TextView tvFoodPercent;
    private TextView tvTransportPercent;
    private TextView tvShoppingPercent;
    private TextView tvOtherPercent;
    private BarChart barChartWeek;

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

        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvFoodPercent = view.findViewById(R.id.tvFoodPercent);
        tvTransportPercent = view.findViewById(R.id.tvTransportPercent);
        tvShoppingPercent = view.findViewById(R.id.tvShoppingPercent);
        tvOtherPercent = view.findViewById(R.id.tvOtherPercent);

        barChartWeek = view.findViewById(R.id.barChartWeek);

        renderTopCategories();
        renderCategorySummary();
        renderWeekChart();
    }

    private void renderTopCategories() {
        LinearLayout container = requireView().findViewById(R.id.layoutTopCategoriesContainer);
        container.removeAllViews();
        List<Expense> expenses = expenseStore.getAllExpenses();

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
        List<Map.Entry<String, Double>> categoryList = new ArrayList<>(amountMap.entrySet());
        Collections.sort(categoryList, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));

        for (Map.Entry<String, Double> entry : categoryList) {
            String category = entry.getKey();
            double amount = entry.getValue();
            int count = countMap.get(category);
            int progress = (int) (amount * 100 / totalAmount);

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

    private void renderCategorySummary() {
        List<Expense> expenses = expenseStore.getAllExpenses();
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
        tvTotalExpense.setText("Tổng\n" + CurrencyUtils.formatVnd(total));
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

    private void renderWeekChart() {
        List<Expense> expenses = expenseStore.getAllExpenses();
        float[] total = new float[7];
        Calendar today = Calendar.getInstance();
        for (Expense expense : expenses) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(expense.getDate());
            int diff = (int) ((today.getTimeInMillis() - c.getTimeInMillis()) / (1000 * 60 * 60 * 24));

            if (diff >= 0 && diff < 7) {
                total[6 - diff] += expense.getAmount();
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, total[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu");
        dataSet.setColor(getResources().getColor(R.color.primary_green));
        dataSet.setValueTextSize(10f);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);

        barChartWeek.setData(data);

        String[] days = {"T-6", "T-5", "T-4", "T-3", "T-2", "Hôm qua", "Hôm nay"};

        XAxis xAxis = barChartWeek.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChartWeek.getAxisRight().setEnabled(false);
        barChartWeek.getDescription().setEnabled(false);
        barChartWeek.getLegend().setEnabled(false);
        barChartWeek.animateY(800);
        barChartWeek.invalidate();
    }
//Statistic tự cập nhật sau khi thêm/sửa/xóa chi tiêu
    @Override
    public void onResume() {
        super.onResume();

        renderTopCategories();
        renderCategorySummary();
        renderWeekChart();
    }
}