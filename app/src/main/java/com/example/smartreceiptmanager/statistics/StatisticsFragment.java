package com.example.smartreceiptmanager.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.expense.ExpenseStore;
import com.example.smartreceiptmanager.utils.CurrencyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private ExpenseStore expenseStore;
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
        renderTopCategories();
    }

    private void renderTopCategories() {
        LinearLayout container = requireView().findViewById(R.id.layoutTopCategoriesContainer);
        container.removeAllViews();
        List<Expense> expenses = expenseStore.getAllExpenses();

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

            if (!amountMap.containsKey(category)) {
                amountMap.put(category, amount);
                countMap.put(category, 1);
            } else {
                amountMap.put(category, amountMap.get(category) + amount);
                countMap.put(category, countMap.get(category) + 1);
            }
        }
        for (String category : amountMap.keySet()) {
            double amount = amountMap.get(category);
            int count = countMap.get(category);
            int progress = (int) ((amount / totalAmount) * 100);

            addTopCategory(container, getCategoryIcon(category), category, count + " giao dịch", CurrencyUtils.formatVnd(amount), progress);
        }
    }

    private String getCategoryIcon(String category) {
        switch (category) {
            case "Ăn uống":
                return "🍽️";
            case "Di chuyển":
                return "🚗";
            case "Mua sắm":
                return "🛍";
            case "Hóa đơn":
                return "🧾";
            default:
                return "💸";
        }
    }

    private void addTopCategory(LinearLayout container, String icon, String name, String count, String amount, int progress) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_top_category, container, false);

        TextView tvIcon = item.findViewById(R.id.tvCategoryIcon);
        TextView tvName = item.findViewById(R.id.tvCategoryName);
        TextView tvCount = item.findViewById(R.id.tvTransactionCount);
        TextView tvAmount = item.findViewById(R.id.tvCategoryAmount);
        ProgressBar progressBar = item.findViewById(R.id.pbIndicator);

        tvIcon.setText(icon);
        tvName.setText(name);
        tvCount.setText(count);
        tvAmount.setText(amount);
        progressBar.setProgress(progress);

        container.addView(item);
    }
}