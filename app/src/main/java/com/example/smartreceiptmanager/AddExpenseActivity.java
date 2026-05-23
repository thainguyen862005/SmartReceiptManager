package com.example.smartreceiptmanager;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddExpenseActivity extends AppCompatActivity {
    private EditText edtTitle, edtAmount, edtDate, edtNote;
    private Spinner spCategory;
    private int editingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        TextView txtScreenTitle = findViewById(R.id.txtScreenTitle);
        edtTitle = findViewById(R.id.edtTitle);
        edtAmount = findViewById(R.id.edtAmount);
        edtDate = findViewById(R.id.edtDate);
        edtNote = findViewById(R.id.edtNote);
        spCategory = findViewById(R.id.spCategory);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnDelete = findViewById(R.id.btnDelete);

        String[] categories = {"Ăn uống", "Mua sắm", "Di chuyển", "Học tập", "Giải trí", "Khác"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(spinnerAdapter);

        editingId = getIntent().getIntExtra("expense_id", -1);
        if (editingId != -1) {
            txtScreenTitle.setText("Sửa khoản chi");
            btnSave.setText("Cập nhật khoản chi");
            btnDelete.setVisibility(android.view.View.VISIBLE);
            fillExpenseData(categories);
        }

        btnSave.setOnClickListener(v -> saveExpense());
        btnCancel.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> {
            if (editingId != -1) {
                ExpenseRepository.delete(editingId);
                Toast.makeText(this, "Đã xóa khoản chi", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void fillExpenseData(String[] categories) {
        Expense expense = ExpenseRepository.findById(editingId);
        if (expense == null) return;
        edtTitle.setText(expense.getTitle());
        edtAmount.setText(String.valueOf((long) expense.getAmount()));
        edtDate.setText(expense.getDate());
        edtNote.setText(expense.getNote());
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(expense.getCategory())) {
                spCategory.setSelection(i);
                break;
            }
        }
    }

    private void saveExpense() {
        String title = edtTitle.getText().toString().trim();
        String amountText = edtAmount.getText().toString().trim();
        String date = edtDate.getText().toString().trim();
        String note = edtNote.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            edtTitle.setError("Vui lòng nhập tên khoản chi");
            return;
        }
        if (TextUtils.isEmpty(amountText)) {
            edtAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        if (TextUtils.isEmpty(date)) {
            edtDate.setError("Vui lòng nhập ngày chi");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            edtAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (editingId == -1) {
            ExpenseRepository.add(title, amount, category, date, note);
            Toast.makeText(this, "Đã thêm khoản chi", Toast.LENGTH_SHORT).show();
        } else {
            ExpenseRepository.update(editingId, title, amount, category, date, note);
            Toast.makeText(this, "Đã cập nhật khoản chi", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
