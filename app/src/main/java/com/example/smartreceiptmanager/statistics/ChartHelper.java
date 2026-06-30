package com.example.smartreceiptmanager.statistics;

import android.content.Context;
import android.graphics.Color;

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

import java.util.ArrayList;


public class ChartHelper {
    public static void renderWeekChart(Context context, BarChart barChartWeek, float[] total) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, total[i]));
        }
        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu");

        dataSet.setColor(ContextCompat.getColor(context, R.color.primary_green));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(context, R.color.text_main));
        dataSet.setHighlightEnabled(false);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);
        barChartWeek.setFitBars(true);
        barChartWeek.setData(data);

        String[] days = {"T-6", "T-5", "T-4", "T-3", "T-2", "Hôm qua", "Hôm nay"};

        XAxis xAxis = barChartWeek.getXAxis();
        barChartWeek.getAxisLeft().setAxisMinimum(0f);
        barChartWeek.getAxisLeft().setDrawGridLines(true);
        barChartWeek.getAxisLeft().setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        barChartWeek.getAxisLeft().setTextSize(10f);

        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelCount(7);
        xAxis.setCenterAxisLabels(false);
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        xAxis.setTextSize(11f);

        barChartWeek.getAxisRight().setEnabled(false);
        barChartWeek.getLegend().setEnabled(false);
        barChartWeek.getDescription().setEnabled(false);
        barChartWeek.animateXY(1000, 800);
        barChartWeek.setScaleEnabled(false);
        barChartWeek.setPinchZoom(false);
        barChartWeek.setDoubleTapToZoomEnabled(false);
        barChartWeek.setDragEnabled(false);
        barChartWeek.setTouchEnabled(false);
        barChartWeek.setExtraBottomOffset(8f);
        barChartWeek.setExtraTopOffset(10f);
        barChartWeek.setExtraLeftOffset(5f);
        barChartWeek.setExtraRightOffset(5f);
        barChartWeek.invalidate();
    }
//PieChart
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
}


