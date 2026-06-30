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
import java.util.LinkedHashMap;
import java.util.Map;


public class ChartHelper {
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


