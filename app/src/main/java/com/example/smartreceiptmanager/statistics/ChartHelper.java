package com.example.smartreceiptmanager.statistics;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class ChartHelper {

    public static void renderDayChart(Context context, BarChart chart, List<Expense> expenses, long fromDate, long toDate) {
        LinkedHashMap<String, Float> dailyMap = new LinkedHashMap<>();
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(fromDate);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        while (current.getTimeInMillis() <= toDate) {
            dailyMap.put(sdf.format(current.getTime()), 0f);
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
        for (Expense expense : expenses) {
            String key = sdf.format(new Date(expense.getDate()));
            if (dailyMap.containsKey(key)) {
                dailyMap.put(key, dailyMap.get(key) + (float) expense.getAmount());
            }
        }
        renderDayChart(context, chart, dailyMap);
    }
    public static void renderDayChart(Context context, BarChart chart,  LinkedHashMap<String,Float> dailyMap) {
        //k có dữ liệu
        chart.clear();

        if (dailyMap == null || dailyMap.isEmpty()) {
            chart.setNoDataText("Không có dữ liệu");
            chart.invalidate();
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int index=0;
        for(Map.Entry<String,Float> entry:dailyMap.entrySet()){
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        BarDataSet dataSet = new BarDataSet(entries,"");

        dataSet.setColor(ContextCompat.getColor(context, R.color.primary_green));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(context, R.color.text_main));
        dataSet.setHighlightEnabled(false);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);
        chart.setFitBars(true);
        chart.setData(data);

        chart.setVisibleXRangeMaximum(10);
        chart.moveViewToX(0);

        XAxis xAxis = chart.getXAxis();
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        chart.getAxisLeft().setTextSize(10f);

        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelCount(labels.size(), false);
        xAxis.setLabelRotationAngle(-30f);
        xAxis.setCenterAxisLabels(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        xAxis.setTextSize(11f);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.animateXY(1000, 800);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setExtraBottomOffset(8f);
        chart.setExtraTopOffset(10f);
        chart.setExtraLeftOffset(5f);
        chart.setExtraRightOffset(5f);
        chart.invalidate();
    }
    //PieChart
    public static void renderPieChart(PieChart pieChart, Context context, List<Expense> expenses) {
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
        renderPieChart(pieChart, context, food, transport, shopping, other, total);
    }
    public static void renderPieChart(PieChart pieChart, Context context, float food, float transport, float shopping, float other, double total){
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
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setCenterText(CurrencyUtils.formatVnd(total));
        pieChart.setCenterTextSize(16f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }


    public static void renderCategorySummary(List<Expense> expenses, TextView tvFoodPercent, TextView tvTransportPercent, TextView tvShoppingPercent, TextView tvOtherPercent) {
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
}


