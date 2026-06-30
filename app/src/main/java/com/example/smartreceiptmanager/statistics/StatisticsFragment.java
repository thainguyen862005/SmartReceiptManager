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
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import android.graphics.Color;

public class StatisticsFragment extends Fragment {

    private ExpenseStore expenseStore;
    private TextView tvTotalExpense;
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
        tabMonth=view.findViewById(R.id.tabMonth);
        tab3Months=view.findViewById(R.id.tab3Months);
        tabYear=view.findViewById(R.id.tabYear);

        //add clickListener
        tabMonth.setOnClickListener(v->{
            currentFilter=FILTER_MONTH;
            refreshStatistics();
        });

        tab3Months.setOnClickListener(v->{
            currentFilter=FILTER_3_MONTHS;
            refreshStatistics();
        });

        tabYear.setOnClickListener(v->{
            currentFilter=FILTER_YEAR;
            refreshStatistics();
        });
    }

    private void refreshStatistics(){
        List<Expense> expenses = getFilteredExpenses();

        renderTopCategories(expenses);
        renderCategorySummary(expenses);
        renderPieChart(expenses);
        renderWeekChart(expenses);

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
                start.set(Calendar.HOUR_OF_DAY,0);
                start.set(Calendar.MINUTE,0);
                start.set(Calendar.SECOND,0);

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

    private void renderCategorySummary(List<Expense> expenses){
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

    private void renderWeekChart(List<Expense> expenses){
        float[] total = new float[7];
        Calendar today = Calendar.getInstance();
        resetTime(today);
        for (Expense expense : expenses) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(expense.getDate());
            resetTime(c);

            long diffMillis = today.getTimeInMillis() - c.getTimeInMillis();
            int diff = (int)(diffMillis / (24*60*60*1000));

            if (diff >= 0 && diff < 7) {
                total[6 - diff] += expense.getAmount();
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, total[i]));
        }

        //cấu hình display cho barChart
        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        dataSet.setHighlightEnabled(false);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);

        barChartWeek.setFitBars(true);
        barChartWeek.setData(data);

        String[] days = {"T-6", "T-5", "T-4", "T-3", "T-2", "Hôm qua", "Hôm nay"};

        //trục XAxis trái
        XAxis xAxis = barChartWeek.getXAxis();
        barChartWeek.getAxisLeft().setAxisMinimum(0f);
        barChartWeek.getAxisLeft().setDrawGridLines(true);
        barChartWeek.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        barChartWeek.getAxisLeft().setTextSize(10f);

        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelCount(7);
        xAxis.setCenterAxisLabels(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        xAxis.setTextSize(11f);

        //trục XAxis phải
        barChartWeek.getAxisRight().setEnabled(false);
        barChartWeek.getDescription().setEnabled(false);
        barChartWeek.getLegend().setEnabled(false);
        barChartWeek.animateXY(1000,800);
        barChartWeek.invalidate();

        //disable zoom dashboard
        barChartWeek.setScaleEnabled(false);
        barChartWeek.setPinchZoom(false);
        barChartWeek.setDoubleTapToZoomEnabled(false);
        barChartWeek.setDragEnabled(false);
        barChartWeek.setTouchEnabled(false);

        //khoảng cách từ viền
        barChartWeek.setExtraBottomOffset(8f);
        barChartWeek.setExtraTopOffset(10f);
        barChartWeek.setExtraLeftOffset(5f);
        barChartWeek.setExtraRightOffset(5f);
    }

    private void renderPieChart(List<Expense> expenses) {
        float food = 0;
        float transport = 0;
        float shopping = 0;
        float other = 0;

        for (Expense expense : expenses) {
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
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (food > 0)
            entries.add(new PieEntry(food, "Ăn uống"));
        if (transport > 0)
            entries.add(new PieEntry(transport, "Di chuyển"));
        if (shopping > 0)
            entries.add(new PieEntry(shopping, "Mua sắm"));
        if (other > 0)
            entries.add(new PieEntry(other, "Khác"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();

        colors.add(Color.parseColor("#4CAF50"));
        colors.add(Color.parseColor("#2196F3"));
        colors.add(Color.parseColor("#FF9800"));
        colors.add(Color.parseColor("#F44336"));

        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setCenterText(CurrencyUtils.formatVnd(expenseStore.getCurrentMonthTotal()));
        pieChart.setCenterTextSize(16f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
// xử lý tránh lệch giờ
    private void resetTime(Calendar calendar){
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
    }

//Statistic tự cập nhật sau khi thêm/sửa/xóa chi tiêu
    @Override
    public void onResume() {
        super.onResume();

        refreshStatistics();
    }
}