package com.example.smartreceiptmanager.expense;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ExpenseStore {
    private static final String PREF_NAME = "expense_store";
    private static final String KEY_EXPENSES = "expenses";
    private static final String KEY_PENDING_DELETED_IDS = "pending_deleted_expense_ids";

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

        removePendingDelete(expense.getId());
        persist(expenses);
    }

    public void deleteExpense(String id) {
        if (id == null) {
            return;
        }

        List<Expense> expenses = getAllExpenses();
        boolean shouldSyncDelete = false;
        for (int i = expenses.size() - 1; i >= 0; i--) {
            Expense expense = expenses.get(i);
            if (id.equals(expense.getId())) {
                shouldSyncDelete = shouldSyncDelete || expense.isSynced();
                expenses.remove(i);
            }
        }
