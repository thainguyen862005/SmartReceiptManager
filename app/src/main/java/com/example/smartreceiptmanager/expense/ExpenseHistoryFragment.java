package com.example.smartreceiptmanager.expense;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseHistoryFragment extends Fragment {

    private LinearLayout layoutAllExpenses;
    private TextView txtEmptyExpenseList;
    private EditText edtSearchExpense;

    private DatabaseReference transactionsRef;
    private DatabaseReference recurringRef;

    private ValueEventListener transactionsListener;
    private ValueEventListener recurringListener;

    private List<Expense> manualExpenses = new ArrayList<>();
    private List<Expense> recurringExpenses = new ArrayList<>();
    private String currentSearchQuery = "";

    private final String dbUrl = "https://appmobile-123-default-rtdb.firebaseio.com/";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutAllExpenses = view.findViewById(R.id.layoutAllExpenses);
        txtEmptyExpenseList = view.findViewById(R.id.txtEmptyExpenseList);
        edtSearchExpense = view.findViewById(R.id.edtSearchExpense);

        if (edtSearchExpense != null) {
            edtSearchExpense.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().trim().toLowerCase();
                    mergeAndRenderUI();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        loadCombinedExpensesFromFirebase();
    }

    private void loadCombinedExpensesFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance(dbUrl);

        transactionsRef = database.getReference("User_Profiles").child("transactions").child(uid);
        recurringRef = database.getReference("User_Profiles").child("recurring_transactions").child(uid);

        // ==========================================
        // 1. DATA TỪ BẢNG TRANSACTIONS
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
                            String receiptUrl = data.child("receipt_image_url").getValue(String.class);
                            boolean isScanned = (receiptUrl != null && !receiptUrl.trim().isEmpty());

                            // Gắn id phù hợp để phân loại lúc render
                            expense.setId(isScanned ? "scanned_" + data.getKey() : "manual_" + data.getKey());

                            Object amtObj = data.child("amount").getValue();
                            expense.setAmount(amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0);

                            String categoryName = "Khác";
                            if (data.child("category").exists()) {
                                String name = data.child("category").child("name").getValue(String.class);
                                if (name != null) categoryName = name;
                            }
                            String type = data.child("type").getValue(String.class);
                            if ("income".equals(type)) categoryName = "Thu nhập";
                            expense.setCategory(categoryName);

                            String note = data.child("note").getValue(String.class);
                            expense.setNote(note != null ? note : "");
                            expense.setMerchantName((note != null && !note.trim().isEmpty()) ? note : categoryName);

                            String dateStr = data.child("transaction_date").getValue(String.class);
                            expense.setDate(parseDateStringToLong(dateStr, "yyyy-MM-dd HH:mm:ss"));

                            manualExpenses.add(expense);
                        } catch (Exception e) {
                            Log.e("Firebase_Manual", "Lỗi nạp item: " + e.getMessage());
                        }
                    }
                }
                mergeAndRenderUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // ==========================================
        // 2. DATA TỪ BẢNG RECURRING_TRANSACTIONS
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
                            String receiptUrl = data.child("receipt_image_url").getValue(String.class);
                            boolean isScanned = (receiptUrl != null && !receiptUrl.trim().isEmpty());

                            if (isScanned) {
                                expense.setId("scanned_" + data.getKey());
                                String shopName = data.child("shop_name").getValue(String.class);
                                expense.setMerchantName(shopName != null ? shopName : "Hóa đơn quét");
                                expense.setCategory("Hóa đơn");
                            } else {
                                expense.setId("recurring_" + data.getKey());
                                String freq = data.child("frequency").getValue(String.class);
                                expense.setMerchantName("Định kỳ: " + (freq != null ? freq : "Chưa rõ"));
                                expense.setCategory("Giao dịch định kỳ");
                            }

                            Object amtObj = data.child("amount").getValue();
                            expense.setAmount(amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0);

                            String note = data.child("note").getValue(String.class);
                            expense.setNote(note != null ? note : "");

                            Object createdAtObj = data.child("created_at").getValue();
                            if (createdAtObj instanceof Number) {
                                expense.setDate(((Number) createdAtObj).longValue());
                            } else {
                                expense.setDate(System.currentTimeMillis());
                            }

                            recurringExpenses.add(expense);
                        } catch (Exception e) {
                            Log.e("Firebase_Recurring", "Lỗi nạp item: " + e.getMessage());
                        }
                    }
                }
                mergeAndRenderUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==========================================
    // HÀM LỌC CHUNG
    // ==========================================
    private List<Expense> filterList(List<Expense> list) {
        List<Expense> filtered = new ArrayList<>();
        for (Expense exp : list) {
            String merchantName = exp.getMerchantName() != null ? exp.getMerchantName().toLowerCase() : "";
            String categoryName = exp.getCategory() != null ? exp.getCategory().toLowerCase() : "";
            String note = exp.getNote() != null ? exp.getNote().toLowerCase() : "";

            if (currentSearchQuery.isEmpty() ||
                    merchantName.contains(currentSearchQuery) ||
                    categoryName.contains(currentSearchQuery) ||
                    note.contains(currentSearchQuery)) {
                filtered.add(exp);
            }
        }
        return filtered;
    }

    // ==========================================
    // TÁCH LOGIC RENDER LÀM 2 NGAY TRONG JAVA
    // ==========================================
    private void mergeAndRenderUI() {
        if (!isAdded() || getContext() == null || layoutAllExpenses == null) return;
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        layoutAllExpenses.removeAllViews();

        // Lấy danh sách đã lọc theo từ khóa tìm kiếm
        List<Expense> filteredFromTransactions = filterList(manualExpenses);
        List<Expense> filteredFromRecurring = filterList(recurringExpenses);

        // Phân tách lại đúng mảng: Nhập tay riêng và (Quét mã + Định kỳ) chung
        List<Expense> finalManualList = new ArrayList<>();
        List<Expense> finalScannedAndRecurringList = new ArrayList<>(filteredFromRecurring);

        for (Expense exp : filteredFromTransactions) {
            if (exp.getId() != null && exp.getId().startsWith("scanned_")) {
                finalScannedAndRecurringList.add(exp); // Đưa hóa đơn quét xuống nhóm dưới
            } else {
                finalManualList.add(exp); // Đưa giao dịch nhập tay vào nhóm trên
            }
        }

        if (finalManualList.isEmpty() && finalScannedAndRecurringList.isEmpty()) {
            txtEmptyExpenseList.setVisibility(View.VISIBLE);
            layoutAllExpenses.setVisibility(View.GONE);
            txtEmptyExpenseList.setText(currentSearchQuery.isEmpty() ? "Chưa có dữ liệu chi tiêu." : "Không tìm thấy kết quả phù hợp.");
            return;
        }

        txtEmptyExpenseList.setVisibility(View.GONE);
        layoutAllExpenses.setVisibility(View.VISIBLE);

        // --- PHẦN 1: DANH SÁCH NHẬP TAY ---
        if (!finalManualList.isEmpty()) {
            layoutAllExpenses.addView(createMainTitle(context, "✏️ Giao dịch thủ công"));

            Collections.sort(finalManualList, (e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));
            String currentGroup = "";
            for (Expense exp : finalManualList) {
                String group = DateUtils.formatDate(exp.getDate());
                if (!group.equals(currentGroup)) {
                    currentGroup = group;
                    layoutAllExpenses.addView(createGroupHeader(context, group));
                }
                addExpenseViewToLayout(exp, inflater, context);
            }
        }

        // --- PHẦN 2: DANH SÁCH QUÉT MÃ & ĐỊNH KỲ ---
        if (!finalScannedAndRecurringList.isEmpty()) {
            View titleView = createMainTitle(context, "🧾 Hóa đơn quét & Định kỳ");
            // Thêm khoảng cách (margin top) nếu đã có phần Nhập Tay ở trên
            if (!finalManualList.isEmpty()) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 60, 0, 0);
                titleView.setLayoutParams(params);
            }
            layoutAllExpenses.addView(titleView);

            Collections.sort(finalScannedAndRecurringList, (e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));
            String currentGroup = "";
            for (Expense exp : finalScannedAndRecurringList) {
                String group = DateUtils.formatDate(exp.getDate());
                if (!group.equals(currentGroup)) {
                    currentGroup = group;
                    layoutAllExpenses.addView(createGroupHeader(context, group));
                }
                addExpenseViewToLayout(exp, inflater, context);
            }
        }
    }

    // Tiêu đề to
    private TextView createMainTitle(Context context, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextColor(0xFF2C3E50); // Màu text đậm
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, 16);
        return header;
    }

    // Tiêu đề nhóm ngày (Hôm nay, Hôm qua...)
    private TextView createGroupHeader(Context context, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextColor(0xFF435047);
        header.setTextSize(16);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 12, 0, 12);
        return header;
    }

    private void addExpenseViewToLayout(Expense expense, LayoutInflater inflater, Context context) {
        View itemView = inflater.inflate(R.layout.item_expense, layoutAllExpenses, false);

        TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
        TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
        TextView txtMeta = itemView.findViewById(R.id.txtMeta);
        TextView txtAmount = itemView.findViewById(R.id.txtAmount);

        txtMerchant.setText(expense.getMerchantName());
        txtIcon.setText(getCategoryIcon(expense.getCategory()));
        txtIcon.setBackgroundResource(getCategoryBackground(expense.getCategory()));

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String realTime = timeFormat.format(new Date(expense.getDate()));

        String inputSource = " • ✏️ Nhập tay";
        if (expense.getId() != null) {
            if (expense.getId().startsWith("scanned_")) {
                inputSource = " • 🧾 Quét mã";
            } else if (expense.getId().startsWith("recurring_")) {
                inputSource = " • 🔄 Định kỳ";
            }
        }

        txtMeta.setText(realTime + " • " + expense.getCategory() + inputSource);

        if ("Thu nhập".equals(expense.getCategory())) {
            txtAmount.setText("+ " + CurrencyUtils.formatVnd(expense.getAmount()));
            txtAmount.setTextColor(0xFF006F49);
        } else {
            txtAmount.setText("- " + CurrencyUtils.formatVnd(expense.getAmount()));
            txtAmount.setTextColor(0xFFB9181E);
        }

        itemView.setOnClickListener(v -> {
            if (getActivity() != null) {
                String originalId = expense.getId() != null ? expense.getId() : "";
                if (originalId.startsWith("scanned_")) originalId = originalId.replace("scanned_", "");
                else if (originalId.startsWith("recurring_")) originalId = originalId.replace("recurring_", "");
                else if (originalId.startsWith("manual_")) originalId = originalId.replace("manual_", "");

                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, ExpenseDetailFragment.newInstance(originalId))
                        .addToBackStack(null)
                        .commit();

                View bottomNav = getActivity().findViewById(R.id.custom_bottom_nav);
                if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            }
        });

        layoutAllExpenses.addView(itemView);
    }

    private String getCategoryIcon(String category) {
        if ("Ăn uống".equals(category)) return "🍴";
        if ("Di chuyển".equals(category)) return "🚗";
        if ("Mua sắm".equals(category)) return "🛍";
        if ("Hóa đơn".equals(category) || "Giao dịch định kỳ".equals(category)) return "🧾";
        if ("Thu nhập".equals(category)) return "💵";
        return "💸";
    }

    private int getCategoryBackground(String category) {
        // Chỉ gọi duy nhất file màu nền mà bạn đang có sẵn trong XML
        // để đảm bảo app không bị văng (ResourceNotFoundException)
        return R.drawable.bg_avatar_mint;
    }

    private long parseDateStringToLong(String dateStr, String format) {
        if (dateStr == null) return System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
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
        if (transactionsRef != null && transactionsListener != null) {
            transactionsRef.removeEventListener(transactionsListener);
        }
        if (recurringRef != null && recurringListener != null) {
            recurringRef.removeEventListener(recurringListener);
        }
        layoutAllExpenses = null;
        txtEmptyExpenseList = null;
        edtSearchExpense = null;
    }
}