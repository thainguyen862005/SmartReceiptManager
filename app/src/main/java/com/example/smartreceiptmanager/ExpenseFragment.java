package com.example.smartreceiptmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ExpenseFragment extends Fragment {

    private TextView txtTotal, txtCount, txtEmpty;
    private RecyclerView listExpense;
    private ExpenseAdapter adapter;
    private ArrayList<Expense> expenses;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public ExpenseFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtTotal = view.findViewById(R.id.txtTotal);
        txtCount = view.findViewById(R.id.txtCount);
        txtEmpty = view.findViewById(R.id.txtEmpty);
        listExpense = view.findViewById(R.id.listExpense);
        Button btnAddExpense = view.findViewById(R.id.btnAddExpense);
        Button btnScan = view.findViewById(R.id.btnScan);

        btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddExpenseActivity.class))
        );

        btnScan.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Chức năng quét hóa đơn sẽ được tích hợp ở module OCR", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void loadExpenses() {
        expenses = ExpenseRepository.getAll();
        adapter = new ExpenseAdapter(requireActivity(), expenses);
        listExpense.setAdapter(adapter);

        txtTotal.setText(currencyFormat.format(ExpenseRepository.getTotalAmount()));
        txtCount.setText(expenses.size() + " khoản chi");
        txtEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa khoản chi")
                .setMessage("Bạn có chắc muốn xóa \"" + expense.getTitle() + "\" không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    ExpenseRepository.delete(expense.getId());
                    loadExpenses();
                    Toast.makeText(requireContext(), "Đã xóa khoản chi", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
