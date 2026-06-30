package com.example.smartreceiptmanager.expense;


import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        } catch (Exception ignored) {
        }
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
    public boolean isDuplicate(String merchant, double amount, long date) {
        merchant = merchant.trim();
        for (Expense e : getAllExpenses()) {
            String oldMerchant = e.getMerchantName() == null ? "" : e.getMerchantName().trim();
            if (oldMerchant.equalsIgnoreCase(merchant) && Math.abs(e.getAmount() - amount) < 0.01 && sameDay(e.getDate(), date)) {
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

    // Lấy danh sách chi tiêu theo khoảng thời gian
    public List<Expense> getExpensesBetween(long start, long end) {
        List<Expense> result = new ArrayList<>();
        for (Expense expense : getAllExpenses()) {
            boolean isAfterStart = expense.getDate() >= start;
            boolean isBeforeEnd = expense.getDate() <= end;

            if (isAfterStart && isBeforeEnd) {
                result.add(expense);
            }
        }

        return result;
    }

    //Tính tổng tiền
    public double getTotalAmount(long start, long end) {
        double total = 0;
        for (Expense expense : getExpensesBetween(start, end)) {
            total += expense.getAmount();
        }
        return total;
    }

    // Tổng tiền tuừng danh mục
    public Map<String, Double> getCategoryTotals(long start, long end) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (Expense expense : getExpensesBetween(start, end)) {
            String category = expense.getCategory();
            double amount = expense.getAmount();

            // Kiểm tra xem danh mục này đã từng được thêm vào Map chưa
            if (result.containsKey(category)) {
                // Nếu đã có=> lấy số tiền cũ ra + số tiền mới
                double oldAmount = result.get(category);
                result.put(category, oldAmount + amount);
            } else {
                // Nếu chưa có => thêm mới danh mục vào Map với số tiền hiện tại
                result.put(category, amount);
            }
        }

        return result;
    }

    //dếm số giao dịch mỗi danh mục
    public Map<String, Integer> getCategoryCounts(long start, long end) {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (Expense expense : getExpensesBetween(start, end)) {
            String category = expense.getCategory();

            // Nếu danh mục đã có trong Map, ta lấy số đếm hiện tại + 1
            if (result.containsKey(category)) {
                int currentCount = result.get(category);
                result.put(category, currentCount + 1);
            } else {
                // Nếu đây là mới ghi nhận số lượng là 1
                result.put(category, 1);
            }
        }
        return result;
    }

    //chi tiêu ừng ngày
    public Map<Integer, Double> getDailyTotals(long start, long end) {
        Map<Integer, Double> result = new LinkedHashMap<>();

        for (Expense expense : getExpensesBetween(start, end)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(expense.getDate());

            int day = calendar.get(Calendar.DAY_OF_MONTH);
            double old = result.containsKey(day) ? result.get(day) : 0;
            result.put(day, old + expense.getAmount());
        }

        return result;
    }

    //Tổng tiền năm hiện tại
    public double getCurrentYearTotal() {
        //Lấy năm hiện tại
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        double total = 0;
        // Khởi tạo Calendar kiểm tra một lần duy nhất ở ngoài vòng lặp
        Calendar expenseCalendar = Calendar.getInstance();

        for (Expense expense : getAllExpenses()) {
            // Tái sử dụng expenseCalendar, chỉ thay đổi thời gian bên trong nó
            expenseCalendar.setTimeInMillis(expense.getDate());

            //So sánh và cộng dồn
            if (expenseCalendar.get(Calendar.YEAR) == currentYear) {
                total += expense.getAmount();
            }
        }

        return total;
    }

    //tỏng tiền 3 tháng gần nhất
    public double getLast3MonthsTotal() {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.MONTH, -2);
        start.set(Calendar.DAY_OF_MONTH, 1);

        long from = start.getTimeInMillis();
        long to = System.currentTimeMillis();

        return getTotalAmount(from, to);
    }

    public List<Expense> getCurrentMonthExpenses() {
        List<Expense> result = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        int year = now.get(Calendar.YEAR);

        for (Expense expense : getAllExpenses()) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(expense.getDate());

            if (c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year) {
                result.add(expense);
            }
        }
        return result;
    }
}
