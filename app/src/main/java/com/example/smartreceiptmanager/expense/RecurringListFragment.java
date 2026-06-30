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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecurringListFragment extends Fragment {

    private LinearLayout layoutAllExpenses;
    private TextView txtEmptyExpenseList;
    private EditText edtSearchExpense;

    private DatabaseReference recurringRef;
    private ValueEventListener recurringListener;

    private List<Expense> recurringList = new ArrayList<>();
    private String currentSearchQuery = "";

    private final String dbUrl = "https://appmobile-123-default-rtdb.firebaseio.com/";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Trỏ đúng vào file XML layout chứa giao diện tổng
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        layoutAllExpenses = view.findViewById(R.id.layoutAllExpenses);
        txtEmptyExpenseList = view.findViewById(R.id.txtEmptyExpenseList);
        edtSearchExpense = view.findViewById(R.id.edtSearchExpense);

        // Đổi text thông báo trống
        if (txtEmptyExpenseList != null) {
            txtEmptyExpenseList.setText("Chưa có giao dịch định kỳ nào.");
        }

        // Xử lý tìm kiếm
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

        loadRecurringData();
    }

    private void loadRecurringData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance(dbUrl);
        
        // Trỏ vào bảng recurring_transactions
        recurringRef = database.getReference("User_Profiles").child("recurring_transactions").child(uid);

        recurringListener = recurringRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                recurringList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            Expense expense = new Expense();
                            expense.setId(data.getKey());

                            Object amtObj = data.child("amount").getValue();
                            expense.setAmount(amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0);

                            String shopName = data.child("shop_name").getValue(String.class);
                            String note = data.child("note").getValue(String.class);
                            expense.setMerchantName(shopName != null && !shopName.isEmpty() ? shopName : "Giao dịch định kỳ");
                            expense.setNote(note != null ? note : "");

                            String frequency = data.child("frequency").getValue(String.class);
                            expense.setCategory(frequency != null ? "Định kỳ: " + frequency : "Giao dịch định kỳ");

                            Object createdAtObj = data.child("created_at").getValue();
                            if (createdAtObj instanceof Number) {
                                expense.setDate(((Number) createdAtObj).longValue());
                            } else {
                                expense.setDate(System.currentTimeMillis());
                            }

                            recurringList.add(expense);
                        } catch (Exception e) {
                            Log.e("Firebase_Recurring", "Lỗi nạp item: " + e.getMessage());
                        }
                    }
                }
                renderUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase_Recurring", "Lỗi Firebase: " + error.getMessage());
            }
        });
    }

    private void renderUI() {
        if (!isAdded() || getContext() == null || layoutAllExpenses == null) return;
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        layoutAllExpenses.removeAllViews();

        List<Expense> filteredList = new ArrayList<>();
        for (Expense exp : recurringList) {
            String merchant = exp.getMerchantName() != null ? exp.getMerchantName().toLowerCase() : "";
            String category = exp.getCategory() != null ? exp.getCategory().toLowerCase() : "";
            
            if (currentSearchQuery.isEmpty() || merchant.contains(currentSearchQuery) || category.contains(currentSearchQuery)) {
                filteredList.add(exp);
            }
        }

        if (filteredList.isEmpty()) {
            txtEmptyExpenseList.setVisibility(View.VISIBLE);
            layoutAllExpenses.setVisibility(View.GONE);
            txtEmptyExpenseList.setText(currentSearchQuery.isEmpty() ? "Chưa có giao dịch định kỳ nào." : "Không tìm thấy kết quả.");
            return;
        }

        txtEmptyExpenseList.setVisibility(View.GONE);
        layoutAllExpenses.setVisibility(View.VISIBLE);

        Collections.sort(filteredList, (e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));

        SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (Expense exp : filteredList) {
            View itemView = inflater.inflate(R.layout.item_expense, layoutAllExpenses, false);

            TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
            TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
            TextView txtMeta = itemView.findViewById(R.id.txtMeta);
            TextView txtAmount = itemView.findViewById(R.id.txtAmount);

            txtMerchant.setText(exp.getMerchantName());
            String dateString = timeFormat.format(new Date(exp.getDate()));
            txtMeta.setText("Tạo ngày: " + dateString + " • " + exp.getCategory());
            
            txtAmount.setText("- " + CurrencyUtils.formatVnd(exp.getAmount()));
            txtAmount.setTextColor(0xFFB9181E); 

            // Ép cứng để không lỗi văng app
            txtIcon.setText("🔄");
            txtIcon.setBackgroundResource(R.drawable.bg_avatar_mint); 

            itemView.setOnClickListener(v -> {
                Toast.makeText(context, "ID: " + exp.getId(), Toast.LENGTH_SHORT).show();
            });

            layoutAllExpenses.addView(itemView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recurringRef != null && recurringListener != null) {
            recurringRef.removeEventListener(recurringListener);
        }
        layoutAllExpenses = null;
        txtEmptyExpenseList = null;
        edtSearchExpense = null;
    }
}