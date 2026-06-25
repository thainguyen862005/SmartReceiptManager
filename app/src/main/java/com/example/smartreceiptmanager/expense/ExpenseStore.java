package com.example.smartreceiptmanager.expense;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ExpenseStore {
    private static final String PREF_NAME = "expense_store";
    private static final String KEY_EXPENSES = "expenses";

    private final SharedPreferences preferences;

    public ExpenseStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<Expense> getAllExpenses() {
        List<Expense> expenses = new ArrayList<>();
        String raw = preferences.getString(KEY_EXPENSES, "[]");

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                expenses.add(fromJson(item));
            }
        } catch (Exception ignored) {
        }

        Collections.sort(expenses, (first, second) -> Long.compare(second.getDate(), first.getDate()));
        return expenses;
    }

    public Expense getExpenseById(String id) {
        if (id == null) {
            return null;
        }

        for (Expense expense : getAllExpenses()) {
            if (id.equals(expense.getId())) {
                return expense;
            }
        }

        return null;
    }

    public void saveExpense(Expense expense) {
        List<Expense> expenses = getAllExpenses();
        long now = System.currentTimeMillis();

        if (expense.getId() == null || expense.getId().trim().isEmpty()) {
            expense.setId(UUID.randomUUID().toString());
            expense.setCreatedAt(now);
        }

        expense.setUpdatedAt(now);
        expense.setSynced(false);

        boolean updated = false;
        for (int i = 0; i < expenses.size(); i++) {
            if (expense.getId().equals(expenses.get(i).getId())) {
                expenses.set(i, expense);
                updated = true;
                break;
            }
        }

        if (!updated) {
            expenses.add(expense);
        }

        persist(expenses);
    }

    public void deleteExpense(String id) {
        if (id == null) {
            return;
        }

        List<Expense> expenses = getAllExpenses();
        for (int i = expenses.size() - 1; i >= 0; i--) {
            if (id.equals(expenses.get(i).getId())) {
                expenses.remove(i);
            }
        }

        persist(expenses);
    }

    public double getCurrentMonthTotal() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        double total = 0;

        for (Expense expense : getAllExpenses()) {
            java.util.Calendar itemDate = java.util.Calendar.getInstance();
            itemDate.setTimeInMillis(expense.getDate());
            if (itemDate.get(java.util.Calendar.MONTH) == currentMonth
                    && itemDate.get(java.util.Calendar.YEAR) == currentYear) {
                total += expense.getAmount();
            }
        }

        return total;
    }

    private void persist(List<Expense> expenses) {
        Collections.sort(expenses, Comparator.comparingLong(Expense::getDate).reversed());
        JSONArray array = new JSONArray();

        try {
            for (Expense expense : expenses) {
                array.put(toJson(expense));
            }
        } catch (Exception ignored) {
        }

        preferences.edit().putString(KEY_EXPENSES, array.toString()).apply();
    }

    private JSONObject toJson(Expense expense) throws Exception {
        JSONObject object = new JSONObject();
        object.put("id", expense.getId());
        object.put("merchantName", expense.getMerchantName());
        object.put("amount", expense.getAmount());
        object.put("category", expense.getCategory());
        object.put("date", expense.getDate());
        object.put("note", expense.getNote());
        object.put("receiptText", expense.getReceiptText());
        object.put("synced", expense.isSynced());
        object.put("createdAt", expense.getCreatedAt());
        object.put("updatedAt", expense.getUpdatedAt());
        return object;
    }

    private Expense fromJson(JSONObject object) {
        return new Expense(
                object.optString("id"),
                object.optString("merchantName"),
                object.optDouble("amount"),
                object.optString("category", "Khác"),
                object.optLong("date", System.currentTimeMillis()),
                object.optString("note"),
                object.optString("receiptText"),
                object.optBoolean("synced"),
                object.optLong("createdAt"),
                object.optLong("updatedAt")
        );
    }

    //tránh lưu trùng 2 lần
    public boolean isDuplicate(String merchant, double amount, long date) {
        List<Expense> expenses = getAllExpenses();
        for (Expense e : expenses) {
            if (e.getMerchantName().equalsIgnoreCase(merchant) && e.getAmount() == amount && sameDay(e.getDate(), date)) {
                return true;
            }
        }
        return false;
    }
    private boolean sameDay(long first, long second) {
        java.util.Calendar c1 = java.util.Calendar.getInstance();
        java.util.Calendar c2 = java.util.Calendar.getInstance();
        c1.setTimeInMillis(first);
        c2.setTimeInMillis(second);
        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
                && c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR);
    }
}
