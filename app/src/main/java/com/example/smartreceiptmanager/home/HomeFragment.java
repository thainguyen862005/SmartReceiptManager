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
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.example.smartreceiptmanager.utils.DateUtils;

// IMPORT CÁC THƯ VIỆN FIREBASE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private TextView txtBalance;
    private TextView txtDayTotal;
    private TextView txtEmptyExpense;
    private LinearLayout layoutExpenseList;
    private LinearLayout layoutWeekChart;
    private AuthViewModel authViewModel;
    private ImageView imgHeaderAvatar;
    private View cardHeaderAvatar;

    // Các biến lắng nghe Firebase để giải phóng bộ nhớ khi đóng Fragment
    private DatabaseReference transactionsRef;
    private DatabaseReference walletRef;
    private ValueEventListener transactionsListener;
    private ValueEventListener walletListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardHeaderAvatar = view.findViewById(R.id.cardHeaderAvatar);
        imgHeaderAvatar = view.findViewById(R.id.imgHeaderAvatar);
        txtBalance = view.findViewById(R.id.txtBalance);
        txtDayTotal = view.findViewById(R.id.txtDayTotal);
        txtEmptyExpense = view.findViewById(R.id.txtEmptyExpense);
        layoutExpenseList = view.findViewById(R.id.layoutExpenseList);
        layoutWeekChart = view.findViewById(R.id.layoutWeekChart);

        // 1. Quản lý thông tin Avatar người dùng
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        authViewModel.getUserProfileLiveData().observe(getViewLifecycleOwner(), userProfile -> {
            if (userProfile != null && userProfile.getProfile() != null && imgHeaderAvatar != null) {
                String avatarUrl = userProfile.getProfile().getAvatar_url();
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    if (avatarUrl.startsWith("http")) {
                        Glide.with(this).load(avatarUrl).placeholder(android.R.drawable.sym_def_app_icon).circleCrop().into(imgHeaderAvatar);
                    } else {
                        try {
                            byte[] bytes = android.util.Base64.decode(avatarUrl, android.util.Base64.DEFAULT);
                            Glide.with(this).load(bytes).placeholder(android.R.drawable.sym_def_app_icon).circleCrop().into(imgHeaderAvatar);
                        } catch (Exception ignored) {}
                    }
                }
            }
        });

        if (cardHeaderAvatar != null) {
            cardHeaderAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                startActivity(intent);
            });
        }

        // 2. Chuyển Tab Lịch sử khi nhấn "Xem tất cả"
        view.findViewById(R.id.btnViewAll).setOnClickListener(v -> {
            requireActivity().findViewById(R.id.btnHistory).performClick();
        });

        // 3. Khởi chạy tiến trình kết nối & đọc dữ liệu từ Firebase
        setupFirebaseRealtime();
    }

    private void setupFirebaseRealtime() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            txtEmptyExpense.setVisibility(View.VISIBLE);
            txtEmptyExpense.setText("Vui lòng đăng nhập lại");
            return;
        }

        String uid = currentUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();


        // --- ĐOẠN ĐỌC DANH SÁCH GIAO DỊCH TỪ FIREBASE ---
        transactionsRef = database.getReference("User_Profiles").child("transactions").child(uid);
        transactionsListener = transactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Expense> firebaseExpenses = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String type = snapshot.child("type").getValue(String.class);
                    if (!"expense".equals(type)) continue;

                    String id = snapshot.getKey();
                    Long amount = snapshot.child("amount").getValue(Long.class);
                    String note = snapshot.child("note").getValue(String.class);
                    String categoryName = snapshot.child("category").child("name").getValue(String.class);

                    // ĐỒNG BỘ NGÀY với ExpenseHistoryFragment: ưu tiên đọc "transaction_date"
                    long finalTime;
                    Object dateObj = snapshot.child("transaction_date").getValue();
                    if (dateObj instanceof String) {
                        long parsed = parseDateStringToLong((String) dateObj);
                        if (parsed > 0) {
                            finalTime = parsed;
                        } else {
                            Long createdAt = snapshot.child("created_at").getValue(Long.class);
                            finalTime = (createdAt != null) ? createdAt : 0;
                        }
                    } else if (dateObj instanceof Number) {
                        finalTime = ((Number) dateObj).longValue();
                    } else {
                        Long createdAt = snapshot.child("created_at").getValue(Long.class);
                        finalTime = (createdAt != null) ? createdAt : 0;
                    }

                    long finalAmount = (amount != null) ? amount : 0;
                    String merchant = (note != null && !note.isEmpty()) ? note : "Chi tiêu không tên";

                    Expense expense = new Expense(id, merchant, finalAmount, categoryName, finalTime, note, "", false, finalTime, finalTime);
                    firebaseExpenses.add(expense);
                }

                // Gộp thêm recurring_transactions rồi mới render (giống Statistics)
                loadRecurringAndRender(uid, firebaseExpenses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(requireContext(), "Lỗi tải Firebase: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadRecurringAndRender(String uid, List<Expense> baseExpenses) {
        DatabaseReference recurringRef = FirebaseDatabase.getInstance()
                .getReference("User_Profiles")
                .child("recurring_transactions")
                .child(uid);

        recurringRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                List<Expense> allExpenses = new ArrayList<>(baseExpenses);

                for (DataSnapshot data : snapshot.getChildren()) {
                    long dateLong = System.currentTimeMillis();
                    Object createdAtObj = data.child("created_at").getValue();

                    if (createdAtObj instanceof Number) {
                        dateLong = ((Number) createdAtObj).longValue();
                    } else if (createdAtObj instanceof String) {
                        dateLong = parseDateStringToLong((String) createdAtObj);
                    }

                    double amount = 0;
                    Object amtObj = data.child("amount").getValue();
                    if (amtObj instanceof Number) amount = ((Number) amtObj).doubleValue();
                    else if (amtObj instanceof String) {
                        try { amount = Double.parseDouble((String) amtObj); } catch (Exception ignored) {}
                    }

                    String id = data.getKey();
                    Expense recurringExp = new Expense(
                            id, "Hóa đơn định kỳ", (long) amount, "Hóa đơn",
                            dateLong, "", "", false, dateLong, dateLong
                    );
                    allExpenses.add(recurringExp);
                }

                long totalAllExpense = 0;
                for (Expense e : allExpenses) {
                    totalAllExpense += (long) e.getAmount();
                }

                renderExpenses(allExpenses, totalAllExpense, uid);
                renderWeekChart(allExpenses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                long totalBase = 0;
                for (Expense e : baseExpenses) totalBase += (long) e.getAmount();
                renderExpenses(baseExpenses, totalBase, uid);
                renderWeekChart(baseExpenses);
            }
        });
    }

    // Đồng bộ format parse ngày với ExpenseHistoryFragment ("yyyy-MM-dd HH:mm:ss")
    private long parseDateStringToLong(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return -1;

        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "dd/MM/yyyy HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(dateStr.trim());
                if (date != null) return date.getTime();
            } catch (ParseException ignored) {}
        }
        return -1;
    }

    /*
    chi tiêu tronng ngày
     */
    private double getTodayTotal(List<Expense> expenses) {
        Calendar today = Calendar.getInstance();
        double total = 0;
        for (Expense expense : expenses) {
            Calendar expDate = Calendar.getInstance();
            expDate.setTimeInMillis(expense.getDate());
            if (today.get(Calendar.YEAR) == expDate.get(Calendar.YEAR)
                    && today.get(Calendar.DAY_OF_YEAR) == expDate.get(Calendar.DAY_OF_YEAR)) {
                total += expense.getAmount();
            }
        }
        return total;
    }
    private void renderExpenses(List<Expense> expenses, long totalExpense, String uid) {
        layoutExpenseList.removeAllViews();

        txtBalance.setText(CurrencyUtils.formatVnd(totalExpense));
        txtDayTotal.setText(CurrencyUtils.formatVnd((long) getTodayTotal(expenses)));

        if (expenses.isEmpty()) {
            txtEmptyExpense.setVisibility(View.VISIBLE);
            return;
        }

        txtEmptyExpense.setVisibility(View.GONE);
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
                if (transactionsRef != null) {
                    transactionsRef.child(expense.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Đã xóa khoản chi", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
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
        long anchorDate = System.currentTimeMillis();
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
        if ("Ăn uống".equals(category)) return "🍴";
        if ("Di chuyển".equals(category)) return "🚗";
        if ("Mua sắm".equals(category)) return "🛍";
        if ("Hóa đơn".equals(category)) return "🧾";
        return "💸";
    }

    private int getCategoryBackground(String category) {
        if ("Ăn uống".equals(category)) return R.drawable.bg_avatar_mint;
        if ("Di chuyển".equals(category)) return R.drawable.bg_avatar_blue;
        return R.drawable.bg_avatar_gray;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionsRef != null && transactionsListener != null) {
            transactionsRef.removeEventListener(transactionsListener);
        }
        if (walletRef != null && walletListener != null) {
            walletRef.removeEventListener(walletListener);
        }
    }
}