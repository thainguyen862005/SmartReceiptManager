package com.example.smartreceiptmanager.statistics;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.expense.ExpenseStore;
import com.example.smartreceiptmanager.utils.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private TextView tvFromDate;
    private TextView tvToDate;

    private long fromDate;
    private long toDate;

    //lưu các tab thống kê
    private static final int FILTER_MONTH = 0;


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
        Calendar monthStart = Calendar.getInstance();

        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);

        fromDate = monthStart.getTimeInMillis();
        toDate = System.currentTimeMillis();

        tvFoodPercent = view.findViewById(R.id.tvFoodPercent);
        tvTransportPercent = view.findViewById(R.id.tvTransportPercent);
        tvShoppingPercent = view.findViewById(R.id.tvShoppingPercent);
        tvOtherPercent = view.findViewById(R.id.tvOtherPercent);

        barChartWeek = view.findViewById(R.id.barChartWeek);
        pieChart = view.findViewById(R.id.pieChart);

        //3 tab filter
        tabMonth = view.findViewById(R.id.tabMonth);
        //add clickListener
        tabMonth.setOnClickListener(v -> {

            Calendar firstDay  = Calendar.getInstance();

            firstDay .set(Calendar.DAY_OF_MONTH, 1);
            firstDay .set(Calendar.HOUR_OF_DAY, 0);
            firstDay .set(Calendar.MINUTE, 0);
            firstDay .set(Calendar.SECOND, 0);
            firstDay .set(Calendar.MILLISECOND, 0);

            fromDate = firstDay .getTimeInMillis();
            toDate = System.currentTimeMillis();

            updateDateText();
            refreshStatistics();
        });

        tvFromDate = view.findViewById(R.id.tvFromDate);
        tvFromDate.setOnClickListener(v->{
            showFromDatePicker();
        });

        tvToDate = view.findViewById(R.id.tvToDate);
        tvToDate.setOnClickListener(v->{
            showToDatePicker();
        });

        Button btnApply = view.findViewById(R.id.btnApplyFilter);
        btnApply.setOnClickListener(v -> {
            if(fromDate > toDate){
                Toast.makeText(requireContext(), "Ngày bắt đầu phải nhỏ hơn ngày kết thúc", Toast.LENGTH_SHORT).show();
                return;
            }
            refreshStatistics();
        });

        refreshStatistics();
    }

    private void refreshStatistics() {
        List<Expense> expenses = expenseStore.getExpensesBetween(fromDate, toDate);

        renderTopCategories(expenses);
        renderCategorySummary(expenses);
        renderPieChart(expenses);
        renderDayChart(expenses);
        updateTabUI();
        updateDateText();
    }

    // lấy data theo tab filter

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

    private void renderDayChart(List<Expense> expenses) {
        LinkedHashMap<String, Float> dailyMap = new LinkedHashMap<>();
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(fromDate);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        // tạo sẵn toàn bộ ngày
        while (current.getTimeInMillis() <= toDate) {
            dailyMap.put(sdf.format(current.getTime()), 0f);
            current.add(Calendar.DAY_OF_MONTH,1);
        }

        // cộng tiền
        for (Expense expense : expenses){
            String key = sdf.format(new Date(expense.getDate()));
            if(dailyMap.containsKey(key)){
                dailyMap.put(key, dailyMap.get(key)+(float)expense.getAmount());
            }

        }
        ChartHelper.renderDayChart(requireContext(), barChartWeek, dailyMap);
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
        tabMonth.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_surface));

        tabMonth.setTypeface(null, Typeface.BOLD);

        tabMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
    }
    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvFromDate.setText(sdf.format(new Date(fromDate)));
        tvToDate.setText(sdf.format(new Date(toDate)));
    }

    private void showFromDatePicker() {
        // lấy ngày tháng năm mặc định
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(fromDate);

        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        // action evet khi người dùng chọn xong
        DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                // Khi người dùng bấm "OK", hàm này sẽ được gọi.

                // đtượng Calendar mới để chứa ngày vừa chọn
                Calendar pickedCalendar = Calendar.getInstance();

                // Cài đặt ngày/tháng/năm vừa chọn. Đặt giờ/phút/giây về 0 để đồng nhất
                pickedCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
                pickedCalendar.set(Calendar.MILLISECOND, 0);

                // update lại biến fromDate (dạng mili-s)
                fromDate = pickedCalendar.getTimeInMillis();
                updateDateText();
            }
        };

        // khởi taạo hợp chọn ngaày
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), onDateSetListener, currentYear, currentMonth, currentDay);

        //show
        dialog.show();
    }

    private void showToDatePicker() {
        // lấy ngày tháng năm mặc định
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(toDate);

        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        // action evet khi người dùng chọn xong
        DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                // Khi người dùng bấm "OK", hàm này sẽ được gọi.

                // đtượng Calendar mới để chứa ngày vừa chọn
                Calendar pickedCalendar = Calendar.getInstance();

                // Cài đặt ngày/tháng/năm vừa chọn. Đặt giờ/phút/giây về 0 để đồng nhất
                pickedCalendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59);

                pickedCalendar.set(Calendar.MILLISECOND,999);

                // update lại biến fromDate (dạng mili-s)
                toDate = pickedCalendar.getTimeInMillis();
                updateDateText();
            }
        };

        // khởi taạo hợp chọn ngaày
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), onDateSetListener, currentYear, currentMonth, currentDay);

        //show
        dialog.show();
    }

    //Statistic tự cập nhật sau khi thêm/sửa/xóa chi tiêu
    @Override
    public void onResume() {
        super.onResume();
        refreshStatistics();
    }
}