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

    private static final String TAG = "ExpenseHistoryDebug";

    private LinearLayout layoutAllExpenses;
    private TextView txtEmptyExpenseList;
    private EditText edtSearchExpense;

    private DatabaseReference transactionsRef;
    private ValueEventListener transactionsListener;

    private List<Expense> expenseList = new ArrayList<>();
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
                    renderUI();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        loadExpensesFromFirebase();
    }

    private void loadExpensesFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "🔴 Auth: Chưa đăng nhập");
            txtEmptyExpenseList.setVisibility(View.VISIBLE);
            txtEmptyExpenseList.setText("Vui lòng đăng nhập để xem lịch sử.");
            return;
        }

        String uid = currentUser.getUid();
        transactionsRef = FirebaseDatabase.getInstance(dbUrl)
                .getReference("User_Profiles").child("transactions").child(uid);

        transactionsListener = transactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                expenseList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            Expense expense = new Expense();

                            // Phân biệt nhập tay và quét mã dựa trên url ảnh
                            Object urlObj = data.child("receipt_image_url").getValue();
                            String receiptUrl = (urlObj instanceof String) ? (String) urlObj : "";
                            boolean isScanned = (!receiptUrl.trim().isEmpty());

                            expense.setId(isScanned ? "scanned_" + data.getKey() : "manual_" + data.getKey());

                            // Đọc số tiền
                            Object amtObj = data.child("amount").getValue();
                            if (amtObj instanceof Number) {
                                expense.setAmount(((Number) amtObj).doubleValue());
                            } else if (amtObj instanceof String) {
                                try { expense.setAmount(Double.parseDouble((String) amtObj)); } catch (Exception e) { expense.setAmount(0.0); }
                            } else {
                                expense.setAmount(0.0);
                            }

                            // Đọc danh mục và phân loại thu/chi
                            String categoryName = "Khác";
                            if (data.child("category").exists()) {
                                Object nameObj = data.child("category").child("name").getValue();
                                if (nameObj instanceof String) categoryName = (String) nameObj;
                            }
                            Object typeObj = data.child("type").getValue();
                            if (typeObj instanceof String && "income".equals(typeObj)) {
                                categoryName = "Thu nhập";
                            }
                            expense.setCategory(categoryName);

                            // Ghi chú và tên cửa hàng
                            Object noteObj = data.child("note").getValue();
                            String note = (noteObj instanceof String) ? (String) noteObj : "";
                            expense.setNote(note);
                            expense.setMerchantName(!note.trim().isEmpty() ? note : categoryName);

                            // Ngày giờ
                            Object dateObj = data.child("transaction_date").getValue();
                            if (dateObj instanceof String) {
                                expense.setDate(parseDateStringToLong((String) dateObj, "yyyy-MM-dd HH:mm:ss"));
                            } else if (dateObj instanceof Number) {
                                expense.setDate(((Number) dateObj).longValue());
                            } else {
                                expense.setDate(System.currentTimeMillis());
                            }

                            expenseList.add(expense);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Lỗi parse ID: " + data.getKey() + " - " + e.getMessage());
                        }
                    }
                }
                renderUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Hủy đọc DB: " + error.getMessage());
            }
        });
    }

    private void renderUI() {
        if (!isAdded() || getContext() == null || layoutAllExpenses == null) return;
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        layoutAllExpenses.removeAllViews();

        // Lọc danh sách theo thanh tìm kiếm
        List<Expense> filteredList = new ArrayList<>();
        for (Expense exp : expenseList) {
            String searchStr = currentSearchQuery.toLowerCase();
            boolean matchMerchant = exp.getMerchantName() != null && exp.getMerchantName().toLowerCase().contains(searchStr);
            boolean matchCategory = exp.getCategory() != null && exp.getCategory().toLowerCase().contains(searchStr);
            boolean matchNote = exp.getNote() != null && exp.getNote().toLowerCase().contains(searchStr);

            if (currentSearchQuery.isEmpty() || matchMerchant || matchCategory || matchNote) {
                filteredList.add(exp);
            }
        }

        if (filteredList.isEmpty()) {
            txtEmptyExpenseList.setVisibility(View.VISIBLE);
            layoutAllExpenses.setVisibility(View.GONE);
            txtEmptyExpenseList.setText(currentSearchQuery.isEmpty() ? "Chưa có dữ liệu chi tiêu." : "Không tìm thấy kết quả phù hợp.");
            return;
        }

        txtEmptyExpenseList.setVisibility(View.GONE);
        layoutAllExpenses.setVisibility(View.VISIBLE);

        // Sắp xếp mới nhất lên đầu
        Collections.sort(filteredList, (e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));

        // Render view kèm header ngày tháng
        String currentGroup = "";
        for (Expense exp : filteredList) {
            String groupDate = DateUtils.formatDate(exp.getDate());
            if (!groupDate.equals(currentGroup)) {
                currentGroup = groupDate;
                layoutAllExpenses.addView(createGroupHeader(context, groupDate));
            }
            addExpenseViewToLayout(exp, inflater, context);
        }
    }

    private TextView createGroupHeader(Context context, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextColor(0xFF435047); // Màu xanh rêu nhạt
        header.setTextSize(16);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 24, 0, 12);
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
        txtIcon.setBackgroundResource(R.drawable.bg_avatar_mint);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String realTime = timeFormat.format(new Date(expense.getDate()));

        String inputSource = expense.getId().startsWith("scanned_") ? " • 🧾 Quét mã" : " • ✏️ Nhập tay";
        txtMeta.setText(realTime + " • " + expense.getCategory() + inputSource);

        if ("Thu nhập".equals(expense.getCategory())) {
            txtAmount.setText("+ " + CurrencyUtils.formatVnd(expense.getAmount()));
            txtAmount.setTextColor(0xFF006F49); // Xanh lá
        } else {
            txtAmount.setText("- " + CurrencyUtils.formatVnd(expense.getAmount()));
            txtAmount.setTextColor(0xFFB9181E); // Đỏ
        }

        itemView.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Tách tiền tố để lấy key gốc trên Firebase truyền qua trang Chi tiết
                String originalId = expense.getId().replace("scanned_", "").replace("manual_", "");

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
        if (category == null) return "💸";
        if (category.equals("Ăn uống")) return "🍴";
        if (category.equals("Di chuyển")) return "🚗";
        if (category.equals("Mua sắm")) return "🛍";
        if (category.equals("Hóa đơn")) return "🧾";
        if (category.equals("Thu nhập")) return "💵";
        return "💸";
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
        layoutAllExpenses = null;
        txtEmptyExpenseList = null;
        edtSearchExpense = null;
    }
}