package com.example.smartreceiptmanager.expense;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.example.smartreceiptmanager.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseListFragment extends Fragment {

    private TextView txtEmpty;
    private LinearLayout layoutAllExpenses;
    private EditText edtSearchExpense;
    private TextView txtMonthFilter;
    private ImageView imgHeaderAvatar;
    private View cardHeaderAvatar;

    private String searchQuery = "";
    private int selectedMonth = -1;

    // Tách 2 list riêng biệt để đọc bất đồng bộ từ Firebase, sau đó gộp vào list tổng
    private List<Expense> manualExpenses = new ArrayList<>();
    private List<Expense> recurringExpenses = new ArrayList<>();
    private List<Expense> allExpenses = new ArrayList<>();

    private DatabaseReference transactionsRef;
    private DatabaseReference recurringRef;
    private ValueEventListener transactionsListener;
    private ValueEventListener recurringListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtEmpty = view.findViewById(R.id.txtEmptyExpenseList);
        layoutAllExpenses = view.findViewById(R.id.layoutAllExpenses);
        edtSearchExpense = view.findViewById(R.id.edtSearchExpense);
        txtMonthFilter = view.findViewById(R.id.txtMonthFilter);
        imgHeaderAvatar = view.findViewById(R.id.imgHeaderAvatar);
        cardHeaderAvatar = view.findViewById(R.id.cardHeaderAvatar);

        if (cardHeaderAvatar != null) {
            cardHeaderAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                startActivity(intent);
            });
        }

        setupSearch();
        loadExpensesFromFirebase();
    }

    private void setupSearch() {
        if (edtSearchExpense == null) return;
        edtSearchExpense.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim();
                renderExpenses();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadExpensesFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            allExpenses = new ArrayList<>();
            renderExpenses();
            return;
        }

        // --- LOAD AVATAR ---
        if (imgHeaderAvatar != null) {
            AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
            authViewModel.getUserLiveData().observe(getViewLifecycleOwner(), firebaseUser -> {
                if (firebaseUser != null && firebaseUser.getPhotoUrl() != null) {
                    Glide.with(this).load(firebaseUser.getPhotoUrl()).placeholder(android.R.drawable.sym_def_app_icon).circleCrop().into(imgHeaderAvatar);
                }
            });
            authViewModel.getUserProfileLiveData().observe(getViewLifecycleOwner(), userProfile -> {
                if (userProfile != null && userProfile.getProfile() != null) {
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
        }

        String uid = currentUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        transactionsRef = database.getReference("User_Profiles").child("transactions").child(uid);
        recurringRef = database.getReference("User_Profiles").child("recurring_transactions").child(uid);

        // ==========================================
        // 1. ĐỌC BẢNG GIAO DỊCH NHẬP TAY (transactions)
        // ==========================================
        transactionsListener = transactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                manualExpenses.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            Expense expense = new Expense();
                            // ĐÁNH DẤU ID: Tiền tố "manual_"
                            expense.setId("manual_" + data.getKey());

                            Double amount = data.child("amount").getValue(Double.class);
                            expense.setAmount(amount != null ? amount : 0);

                            String note = data.child("note").getValue(String.class);
                            expense.setNote(note != null ? note : "");

                            DataSnapshot categorySnap = data.child("category");
                            String categoryName = "Khác";
                            if (categorySnap.exists()) {
                                String name = categorySnap.child("name").getValue(String.class);
                                if (name != null) categoryName = name;
                            }
                            expense.setCategory(categoryName);
                            expense.setMerchantName((note != null && !note.trim().isEmpty()) ? note : categoryName);

                            String type = data.child("type").getValue(String.class);
                            if ("income".equals(type)) expense.setCategory("Thu nhập");

                            String dateStr = data.child("transaction_date").getValue(String.class);
                            expense.setDate(parseDateStringToLong(dateStr));

                            manualExpenses.add(expense);
                        } catch (Exception e) {
                            Log.e("Firebase", "Lỗi parse manual: " + e.getMessage());
                        }
                    }
                }
                combineAndRender(); // Gọi hàm gộp chung
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // ==========================================
        // 2. ĐỌC BẢNG ĐỊNH KỲ / QUÉT MÃ (recurring_transactions)
        // ==========================================
        recurringListener = recurringRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                recurringExpenses.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            Expense expense = new Expense();
                            // ĐÁNH DẤU ID: Tiền tố "recurring_"
                            expense.setId("recurring_" + data.getKey());

                            // Xử lý an toàn số tiền (có thể lưu là int, double, hoặc string)
                            Object amtObj = data.child("amount").getValue();
                            if (amtObj instanceof Number) {
                                expense.setAmount(((Number) amtObj).doubleValue());
                            } else if (amtObj instanceof String) {
                                try { expense.setAmount(Double.parseDouble((String) amtObj)); } catch (Exception e) { expense.setAmount(0.0); }
                            }

                            // Xác định tên hiển thị và danh mục
                            Object shopObj = data.child("shop_name").getValue();
                            String shopName = (shopObj instanceof String && !((String) shopObj).trim().isEmpty())
                                    ? (String) shopObj : "Hóa đơn quét / Định kỳ";
                            expense.setMerchantName(shopName);
                            expense.setCategory("Hóa đơn");

                            Object noteObj = data.child("note").getValue();
                            expense.setNote(noteObj instanceof String ? (String) noteObj : "");

                            // Lấy thời gian từ "created_at"
                            Object createdAtObj = data.child("created_at").getValue();
                            if (createdAtObj instanceof Number) {
                                expense.setDate(((Number) createdAtObj).longValue());
                            } else if (createdAtObj instanceof String) {
                                expense.setDate(parseDateStringToLong((String) createdAtObj));
                            } else {
                                expense.setDate(System.currentTimeMillis());
                            }

                            recurringExpenses.add(expense);
                        } catch (Exception e) {
                            Log.e("Firebase", "Lỗi parse recurring: " + e.getMessage());
                        }
                    }
                }
                combineAndRender(); // Gọi hàm gộp chung
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Gộp 2 list lại, sắp xếp theo thời gian mới nhất lên đầu
    private void combineAndRender() {
        allExpenses.clear();
        allExpenses.addAll(manualExpenses);
        allExpenses.addAll(recurringExpenses);

        Collections.sort(allExpenses, (e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));
        renderExpenses();
    }

    private void renderExpenses() {
        if (txtEmpty == null || layoutAllExpenses == null || !isAdded()) return;

        setupMonthFilter(allExpenses);
        List<Expense> expenses = filterExpenses(allExpenses);
        txtEmpty.setText(getEmptyMessage());
        txtEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
        layoutAllExpenses.removeAllViews();

        if (expenses.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String currentGroup = "";

        for (Expense expense : expenses) {
            String group = DateUtils.formatDate(expense.getDate());
            if (!group.equals(currentGroup)) {
                currentGroup = group;
                layoutAllExpenses.addView(createGroupHeader(group));
            }

            View itemView = inflater.inflate(R.layout.item_expense, layoutAllExpenses, false);
            TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
            TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
            TextView txtMeta = itemView.findViewById(R.id.txtMeta);
            TextView txtAmount = itemView.findViewById(R.id.txtAmount);
            TextView btnDelete = itemView.findViewById(R.id.btnDeleteExpense);

            txtIcon.setText(getCategoryIcon(expense.getCategory()));
            txtIcon.setBackgroundResource(getCategoryBackground(expense.getCategory()));
            txtMerchant.setText(expense.getMerchantName());

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String realTime = timeFormat.format(new Date(expense.getDate()));

            // Thêm nguồn gốc để người dùng dễ phân biệt
            String source = expense.getId().startsWith("recurring_") ? " • 🧾 Quét mã/Định kỳ" : " • ✏️ Nhập tay";
            txtMeta.setText(realTime + " • " + expense.getCategory() + source);

            if ("Thu nhập".equals(expense.getCategory())) {
                txtAmount.setText("+ " + CurrencyUtils.formatVnd(expense.getAmount()));
                txtAmount.setTextColor(getResources().getColor(R.color.primary_green));
            } else {
                txtAmount.setText("- " + CurrencyUtils.formatVnd(expense.getAmount()));
                txtAmount.setTextColor(0xFFB9181E);
            }

            // Gắn sự kiện click
            itemView.setOnClickListener(v -> openExpenseDetail(expense.getId()));
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> openExpenseDetail(expense.getId()));
            }

            layoutAllExpenses.addView(itemView);
        }
    }

    private List<Expense> filterExpenses(List<Expense> expenses) {
        String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT);
        List<Expense> filteredExpenses = new ArrayList<>();
        for (Expense expense : expenses) {
            if (matchesMonth(expense) && matchesSearch(expense, normalizedQuery)) {
                filteredExpenses.add(expense);
            }
        }
        return filteredExpenses;
    }

    private boolean matchesMonth(Expense expense) {
        return selectedMonth == -1 || selectedMonth == getMonth(expense);
    }

    private boolean matchesSearch(Expense expense, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) return true;
        String searchableText = (
                expense.getMerchantName() + " " +
                        expense.getCategory() + " " +
                        expense.getNote() + " " +
                        expense.getReceiptText() + " " +
                        DateUtils.formatDate(expense.getDate()) + " " +
                        CurrencyUtils.formatVnd(expense.getAmount()) + " " +
                        ((long) expense.getAmount())
        ).toLowerCase(Locale.ROOT);
        return searchableText.contains(normalizedQuery);
    }

    private void setupMonthFilter(List<Expense> expenses) {
        if (txtMonthFilter == null) return;
        txtMonthFilter.setText(selectedMonth == -1 ? "≡  Tất cả tháng" : "≡  Tháng " + (selectedMonth + 1));
        txtMonthFilter.setOnClickListener(v -> showMonthMenu());
    }

    private void showMonthMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), txtMonthFilter);
        popupMenu.getMenu().add(0, 0, 0, "Tất cả tháng");
        for (int month = 0; month < 12; month++) {
            popupMenu.getMenu().add(0, month + 1, month + 1, "Tháng " + (month + 1));
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            selectedMonth = item.getItemId() == 0 ? -1 : item.getItemId() - 1;
            renderExpenses();
            return true;
        });
        popupMenu.show();
    }

    private int getMonth(Expense expense) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expense.getDate());
        return calendar.get(Calendar.MONTH);
    }

    private String getEmptyMessage() {
        if (!searchQuery.isEmpty()) return "Không tìm thấy giao dịch phù hợp.";
        if (selectedMonth != -1) return "Không có giao dịch trong tháng đã chọn.";
        return "Chưa có dữ liệu chi tiêu.";
    }

    private TextView createGroupHeader(String title) {
        TextView header = new TextView(requireContext());
        header.setText(title);
        header.setTextColor(0xFF435047);
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 32, 0, 16);
        return header;
    }

    private void openExpenseDetail(String prefixedId) {
        Fragment detailFragment;

        if (prefixedId.startsWith("manual_")) {
            String realId = prefixedId.replace("manual_", "");
            detailFragment = ExpenseDetailFragment.newInstance(realId);
        } else if (prefixedId.startsWith("recurring_")) {
            String realId = prefixedId.replace("recurring_", "");
            // 👇 GỌI SANG TRANG CHI TIẾT CỦA RECURRING
            detailFragment = RecurringDetailFragment.newInstance(realId);
        } else {
            return;
        }

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();

        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }
    private String getCategoryIcon(String category) {
        if ("Ăn uống".equals(category)) return "🍴";
        if ("Di chuyển".equals(category)) return "🚗";
        if ("Mua sắm".equals(category)) return "🛍";
        if ("Hóa đơn".equals(category)) return "🧾";
        if ("Thu nhập".equals(category)) return "💵";
        if ("Sinh hoạt".equals(category)) return "🛍";
        return "💸";
    }

    private int getCategoryBackground(String category) {
        if ("Ăn uống".equals(category) || "Thu nhập".equals(category)) return R.drawable.bg_avatar_green;
        else if ("Di chuyển".equals(category)) return R.drawable.bg_avatar_blue;
        else if ("Sinh hoạt".equals(category)) return R.drawable.bg_avatar_mint;
        return R.drawable.bg_avatar_gray;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy lắng nghe dữ liệu khi thoát màn hình để tránh rò rỉ bộ nhớ
        if (transactionsRef != null && transactionsListener != null) {
            transactionsRef.removeEventListener(transactionsListener);
        }
        if (recurringRef != null && recurringListener != null) {
            recurringRef.removeEventListener(recurringListener);
        }
    }
}