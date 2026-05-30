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

public class ExpenseDetailFragment extends Fragment {
    private static final String ARG_EXPENSE_ID = "expense_id";

    private ExpenseStore expenseStore;
    private Expense expense;

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

        expenseStore = new ExpenseStore(requireContext());
        String expenseId = getArguments() == null ? null : getArguments().getString(ARG_EXPENSE_ID);
        expense = expenseStore.getExpenseById(expenseId);
        if (expense == null) {
            expense = createPreviewExpense();
        }

        ((TextView) view.findViewById(R.id.txtCategoryPill)).setText(getCategoryIcon(expense.getCategory()) + "  " + expense.getCategory());
        ((TextView) view.findViewById(R.id.txtDetailAmount)).setText("-" + CurrencyUtils.formatVnd(expense.getAmount()));
        ((TextView) view.findViewById(R.id.txtDetailDate)).setText("▦ " + DateUtils.formatDate(expense.getDate()) + ", 12:30 PM");

        String note = expense.getNote();
        if (note == null || note.trim().isEmpty()) {
            note = expense.getMerchantName();
        }
        ((TextView) view.findViewById(R.id.txtDetailNote)).setText(note);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> close());
        view.findViewById(R.id.btnEdit).setOnClickListener(v -> openEdit());
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> deleteExpense());
    }

    private void openEdit() {
        if (expense.getId() == null || expense.getId().startsWith("preview")) {
            Toast.makeText(requireContext(), "Đây là dữ liệu mẫu", Toast.LENGTH_SHORT).show();
            return;
        }

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, AddExpenseFragment.newEditInstance(expense.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void deleteExpense() {
        if (expense.getId() == null || expense.getId().startsWith("preview")) {
            Toast.makeText(requireContext(), "Đây là dữ liệu mẫu", Toast.LENGTH_SHORT).show();
            return;
        }

        expenseStore.deleteExpense(expense.getId());
        Toast.makeText(requireContext(), "Đã xóa khoản chi", Toast.LENGTH_SHORT).show();
        close();
    }

    private void close() {
        requireActivity().getSupportFragmentManager().popBackStack();
        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }

    private Expense createPreviewExpense() {
        return new Expense(
                "preview-detail",
                "Ăn trưa Highland",
                125000,
                "Ăn uống",
                System.currentTimeMillis(),
                "Ăn trưa với team dự án mới tại quán phở cuốn Hương Mai. Hóa đơn đã bao gồm phí VAT.",
                "",
                false,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );
    }

    private String getCategoryIcon(String category) {
        if ("Ăn uống".equals(category)) return "🍴";
        if ("Di chuyển".equals(category)) return "🚗";
        if ("Mua sắm".equals(category)) return "🛍";
        if ("Hóa đơn".equals(category)) return "🧾";
        return "💸";
    }
}
