package com.example.smartreceiptmanager.statistics;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.expense.ExpenseStore;

// Thư viện Firebase
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.ParseException;
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

// Biểu đồ MPAndroidChart
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

        tabMonth = view.findViewById(R.id.tabMonth);
        tabMonth.setOnClickListener(v -> {
            Calendar firstDay = Calendar.getInstance();
            firstDay.set(Calendar.DAY_OF_MONTH, 1);
            firstDay.set(Calendar.HOUR_OF_DAY, 0);
            firstDay.set(Calendar.MINUTE, 0);
            firstDay.set(Calendar.SECOND, 0);
            firstDay.set(Calendar.MILLISECOND, 0);

            fromDate = firstDay.getTimeInMillis();
            toDate = System.currentTimeMillis();

            updateDateText();
            refreshStatistics();
        });

        tvFromDate = view.findViewById(R.id.tvFromDate);
        tvFromDate.setOnClickListener(v -> showFromDatePicker());

        tvToDate = view.findViewById(R.id.tvToDate);
        tvToDate.setOnClickListener(v -> showToDatePicker());

        Button btnApply = view.findViewById(R.id.btnApplyFilter);
        btnApply.setOnClickListener(v -> {
            if (fromDate > toDate) {
                Toast.makeText(requireContext(), "Ngày bắt đầu phải nhỏ hơn ngày kết thúc", Toast.LENGTH_SHORT).show();
                return;
            }
            refreshStatistics();
        });

        refreshStatistics();
    }

    // ==========================================
    // LOGIC LẤY & GỘP DỮ LIỆU
    // ==========================================
    private void refreshStatistics() {
        // 1. Lấy dữ liệu Local
        List<Expense> localExpenses = expenseStore.getExpensesBetween(fromDate, toDate);

        // 2. Lấy dữ liệu Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            renderAllCharts(localExpenses);
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("User_Profiles")
                .child("recurring_transactions")
                .child(currentUser.getUid());

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                List<Expense> allExpenses = new ArrayList<>(localExpenses);

                for (DataSnapshot data : snapshot.getChildren()) {
                    long dateLong = System.currentTimeMillis();
                    Object createdAtObj = data.child("created_at").getValue();

                    if (createdAtObj instanceof Number) {
                        dateLong = ((Number) createdAtObj).longValue();
                    } else if (createdAtObj instanceof String) {
                        dateLong = parseDateStringToLong((String) createdAtObj);
                    }

                    // 3. Lọc theo ngày
                    if (dateLong >= fromDate && dateLong <= toDate) {
                        double amount = 0;
                        Object amtObj = data.child("amount").getValue();
                        if (amtObj instanceof Number) amount = ((Number) amtObj).doubleValue();
                        else if (amtObj instanceof String) {
                            try { amount = Double.parseDouble((String) amtObj); } catch (Exception ignored) {}
                        }

                        // Gán vào Model
                        Expense recurringExp = new Expense();
                        recurringExp.setAmount(amount);
                        recurringExp.setCategory("Hóa đơn");
                        recurringExp.setDate(dateLong);

                        allExpenses.add(recurringExp);
                    }
                }
                // 4. Cập nhật UI
                renderAllCharts(allExpenses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    renderAllCharts(localExpenses);
                }
            }
        });
    }

    private void renderAllCharts(List<Expense> expenses) {
        renderTopCategories(expenses);
        renderCategorySummary(expenses);
        renderPieChart(expenses);
        renderDayChart(expenses);
        updateTabUI();
        updateDateText();
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

    // ==========================================
    // PHẦN RENDER GIAO DIỆN "TOP CHI TIÊU" MỚI
    // ==========================================
    private void renderTopCategories(List<Expense> expenses) {
        LinearLayout container = requireView().findViewById(R.id.layoutTopCategoriesContainer);
        if (container != null) {
            container.removeAllViews();
        }

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

            amountMap.put(category, amountMap.getOrDefault(category, 0.0) + amount);
            countMap.put(category, countMap.getOrDefault(category, 0) + 1);
        }

        if (totalAmount == 0) return;

        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(amountMap.entrySet());
        Collections.sort(sortedList, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));

        for (Map.Entry<String, Double> entry : sortedList) {
            String category = entry.getKey();
            double amount = entry.getValue();
            int count = countMap.get(category);
            int progress = (int) (amount / totalAmount * 100);

            addTopCategory(
                    container,
                    getCategoryIcon(category),
                    category,
                    count + " giao dịch",
                    amount,
                    progress,
                    getCategoryColor(category)
            );
        }
    }

    private String getCategoryIcon(String category) {
        switch (category) {
            case "Ăn uống": return "🍽️";
            case "Di chuyển": return "🚗";
            case "Mua sắm": return "🛍";
            case "Hóa đơn": return "🧾";
            default: return "💸";
        }
    }

    private String getCategoryColor(String category) {
        switch (category) {
            case "Ăn uống": return "#FF9800";
            case "Di chuyển": return "#03A9F4";
            case "Mua sắm": return "#E91E63";
            case "Hóa đơn": return "#4CAF50";
            default: return "#9E9E9E";
        }
    }

    private void addTopCategory(LinearLayout container, String icon, String name, String count, double amount, int progress, String hexColor) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_top_category, container, false);

        TextView tvIcon = item.findViewById(R.id.tvCategoryIcon);
        CardView cardIconBg = item.findViewById(R.id.cardIconBg);
        TextView tvName = item.findViewById(R.id.tvCategoryName);
        TextView tvCount = item.findViewById(R.id.tvTransactionCount);
        TextView tvAmount = item.findViewById(R.id.tvCategoryAmount);
        ProgressBar pbIndicator = item.findViewById(R.id.pbIndicator);

        tvIcon.setText(icon);
        tvName.setText(name);
        tvCount.setText(count);

        // Format tiền tệ
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvAmount.setText(currencyFormatter.format(amount));

        pbIndicator.setProgress(progress);

        try {
            int colorInt = Color.parseColor(hexColor);
            pbIndicator.setProgressTintList(ColorStateList.valueOf(colorInt));

            if (cardIconBg != null) {
                cardIconBg.setCardBackgroundColor(colorInt);
                cardIconBg.setBackgroundTintList(ColorStateList.valueOf(colorInt).withAlpha(40));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (container != null) {
            container.addView(item);
        }
    }

    // ==========================================
    // PHẦN RENDER CÁC BIỂU ĐỒ KHÁC
    // ==========================================
    private void renderCategorySummary(List<Expense> expenses) {
        double total = 0;
        double food = 0;
        double transport = 0;
        double shopping = 0;
        double other = 0;

        for (Expense expense : expenses) {
            total += expense.getAmount();
            switch (expense.getCategory()) {
                case "Ăn uống": food += expense.getAmount(); break;
                case "Di chuyển": transport += expense.getAmount(); break;
                case "Mua sắm": shopping += expense.getAmount(); break;
                default: other += expense.getAmount();
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

    private void renderDayChart(List<Expense> expenses) {
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
                case "Ăn uống": food += expense.getAmount(); break;
                case "Di chuyển": transport += expense.getAmount(); break;
                case "Mua sắm": shopping += expense.getAmount(); break;
                default: other += expense.getAmount();
            }
        }
        ChartHelper.renderPieChart(pieChart, requireContext(), food, transport, shopping, other, total);
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH (NGÀY, THÁNG, TAB)
    // ==========================================
    private void updateTabUI() {
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
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(fromDate);

        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog.OnDateSetListener onDateSetListener = (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar pickedCalendar = Calendar.getInstance();
            pickedCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
            pickedCalendar.set(Calendar.MILLISECOND, 0);

            fromDate = pickedCalendar.getTimeInMillis();
            updateDateText();
        };
        new DatePickerDialog(requireContext(), onDateSetListener, currentYear, currentMonth, currentDay).show();
    }

    private void showToDatePicker() {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(toDate);

        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog.OnDateSetListener onDateSetListener = (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar pickedCalendar = Calendar.getInstance();
            pickedCalendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59);
            pickedCalendar.set(Calendar.MILLISECOND, 999);

            toDate = pickedCalendar.getTimeInMillis();
            updateDateText();
        };
        new DatePickerDialog(requireContext(), onDateSetListener, currentYear, currentMonth, currentDay).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatistics();
    }
}