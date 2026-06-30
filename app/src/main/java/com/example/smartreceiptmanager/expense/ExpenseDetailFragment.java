package com.example.smartreceiptmanager.expense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Date;
import java.util.Locale;

public class ExpenseDetailFragment extends Fragment {
    private static final String ARG_EXPENSE_ID = "expense_id";

    private String currentExpenseId;
    private Expense currentExpense;

    private TextView txtCategoryPill;
    private TextView txtDetailAmount;
    private TextView txtDetailDate;
    private TextView txtDetailNote;

    public static ExpenseDetailFragment newInstance(String expenseId) {
        ExpenseDetailFragment fragment = new ExpenseDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXPENSE_ID, expenseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy ID giao dịch được truyền sang từ màn hình Danh sách
        currentExpenseId = getArguments() == null ? null : getArguments().getString(ARG_EXPENSE_ID);

        txtCategoryPill = view.findViewById(R.id.txtCategoryPill);
        txtDetailAmount = view.findViewById(R.id.txtDetailAmount);
        txtDetailDate = view.findViewById(R.id.txtDetailDate);
        txtDetailNote = view.findViewById(R.id.txtDetailNote);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> close());
        view.findViewById(R.id.btnEdit).setOnClickListener(v -> openEdit());
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> deleteExpense());

        // Bắt đầu tải dữ liệu từ Firebase
        if (currentExpenseId != null) {
            loadExpenseFromFirebase();
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy mã giao dịch", Toast.LENGTH_SHORT).show();
            close();
        }
    }

    private void loadExpenseFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Trỏ đúng vào: User_Profiles -> transactions -> UID -> ID của khoản chi này
        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("User_Profiles")
                .child("transactions")
                .child(currentUser.getUid())
                .child(currentExpenseId);

        // Đọc dữ liệu 1 lần duy nhất
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot data) {
                if (data.exists()) {
                    currentExpense = new Expense();
                    currentExpense.setId(data.getKey());

                    Double amount = data.child("amount").getValue(Double.class);
                    currentExpense.setAmount(amount != null ? amount : 0);

                    String note = data.child("note").getValue(String.class);
                    currentExpense.setNote(note != null ? note : "");

                    DataSnapshot categorySnap = data.child("category");
                    String categoryName = "Khác";
                    if (categorySnap.exists()) {
                        String name = categorySnap.child("name").getValue(String.class);
                        if (name != null) categoryName = name;
                    }
                    currentExpense.setCategory(categoryName);

                    // Phân loại Thu/Chi
                    String type = data.child("type").getValue(String.class);
                    if ("income".equals(type)) {
                        currentExpense.setCategory("Thu nhập");
                    }

                    String dateStr = data.child("transaction_date").getValue(String.class);
                    currentExpense.setDate(parseDateStringToLong(dateStr));

                    // Nạp dữ liệu lên giao diện
                    updateUI();
                } else {
                    Toast.makeText(requireContext(), "Khoản chi này đã bị xóa hoặc không tồn tại", Toast.LENGTH_SHORT).show();
                    close();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (currentExpense == null) return;

        txtCategoryPill.setText(getCategoryIcon(currentExpense.getCategory()) + "  " + currentExpense.getCategory());

        // Kiểm tra nếu là Thu nhập thì hiện dấu +
        if ("Thu nhập".equals(currentExpense.getCategory())) {
            txtDetailAmount.setText("+" + CurrencyUtils.formatVnd(currentExpense.getAmount()));
        } else {
            txtDetailAmount.setText("-" + CurrencyUtils.formatVnd(currentExpense.getAmount()));
        }

        txtDetailDate.setText("▦ " + DateUtils.formatDateTime(currentExpense.getDate()));

        String note = currentExpense.getNote();
        if (note == null || note.trim().isEmpty()) {
            note = currentExpense.getCategory();
        }
        txtDetailNote.setText(note);
    }

    private void openEdit() {
        if (currentExpense == null || currentExpense.getId() == null) return;
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, AddExpenseFragment.newEditInstance(currentExpense.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void deleteExpense() {
        if (currentExpenseId == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // XÓA TRỰC TIẾP TRÊN FIREBASE
        FirebaseDatabase.getInstance()
                .getReference("User_Profiles")
                .child("transactions")
                .child(currentUser.getUid())
                .child(currentExpenseId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Đã xóa thành công", Toast.LENGTH_SHORT).show();
                    close(); // Tự động quay về màn hình trước
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void close() {
        requireActivity().getSupportFragmentManager().popBackStack();
        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
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
}