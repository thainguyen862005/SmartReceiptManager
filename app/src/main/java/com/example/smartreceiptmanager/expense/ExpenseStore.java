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
    private final Context context;

    public ExpenseStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

        Collections.sort(expenses, new Comparator<Expense>() {
            @Override
            public int compare(Expense o1, Expense o2) {
                return Long.compare(o2.getDate(), o1.getDate());
            }
        });
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
        checkBudget();
    }

    private void checkBudget() {
        SharedPreferences globalPrefs = context.getSharedPreferences("smart_receipt_prefs", Context.MODE_PRIVATE);
        boolean isBudgetNotifEnabled = globalPrefs.getBoolean("notif_budget", true);
        if (isBudgetNotifEnabled) {
            long budgetLimit = globalPrefs.getLong("budget_limit", 10000000L);
            double currentMonthTotal = getCurrentMonthTotal();
            if (currentMonthTotal >= budgetLimit) {
                com.example.smartreceiptmanager.utils.NotificationHelper.showNotification(
                        context,
                        "Cảnh báo vượt ngân sách",
                        "Tổng chi tiêu tháng này của bạn đã vượt quá hạn mức (" + com.example.smartreceiptmanager.utils.CurrencyUtils.formatVnd(currentMonthTotal) + " / " + com.example.smartreceiptmanager.utils.CurrencyUtils.formatVnd(budgetLimit) + ")!"
                );
            }
        }
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

        persist(expenses);

        if (shouldSyncDelete) {
            addPendingDelete(id);
        }
    }

    public List<String> getPendingDeletedExpenseIds() {
        List<String> ids = new ArrayList<>();
        String raw = preferences.getString(KEY_PENDING_DELETED_IDS, "[]");

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String id = array.optString(i, "");
                if (!id.trim().isEmpty()) {
                    ids.add(id);
                }
            }
        } catch (Exception ignored) {
        }

        return ids;
    }

    public void markDeleteSynced(String id) {
        removePendingDelete(id);
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

        Collections.sort(expenses, new Comparator<Expense>() {
            @Override
            public int compare(Expense o1, Expense o2) {
                return Long.compare(o2.getDate(), o1.getDate());
            }
        });
        JSONArray array = new JSONArray();
        try {
            for (Expense expense : expenses) {
                array.put(toJson(expense));
            }
        } catch (Exception ignored) {}
        preferences.edit().putString(KEY_EXPENSES, array.toString()).commit();
    }

    private void addPendingDelete(String id) {
        List<String> ids = getPendingDeletedExpenseIds();
        if (!ids.contains(id)) {
            ids.add(id);
            persistPendingDeletes(ids);
        }
    }

    private void removePendingDelete(String id) {
        if (id == null) {
            return;
        }

        List<String> ids = getPendingDeletedExpenseIds();
        if (ids.remove(id)) {
            persistPendingDeletes(ids);
        }
    }

    private void persistPendingDeletes(List<String> ids) {
        JSONArray array = new JSONArray();
        for (String id : ids) {
            array.put(id);
        }

        preferences.edit().putString(KEY_PENDING_DELETED_IDS, array.toString()).commit();
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
    public boolean isDuplicate(String merchant,double amount,long date){
        merchant = merchant.trim();
        for(Expense e:getAllExpenses()){
            String oldMerchant = e.getMerchantName()==null ? "" : e.getMerchantName().trim();
            if(oldMerchant.equalsIgnoreCase(merchant) && Math.abs(e.getAmount()-amount)<0.01 && sameDay(e.getDate(),date)){
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
