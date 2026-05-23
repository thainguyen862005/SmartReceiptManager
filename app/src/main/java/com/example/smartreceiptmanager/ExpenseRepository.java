package com.example.smartreceiptmanager;

import java.util.ArrayList;

public class ExpenseRepository {
    private static final ArrayList<Expense> expenses = new ArrayList<>();
    private static int nextId = 6;

    static {
        expenses.add(new Expense(1, "Cà phê sáng", 35000, "Ăn uống", "22/05/2026", "Highlands Coffee"));
        expenses.add(new Expense(2, "Mua sách Android", 125000, "Học tập", "21/05/2026", "Sách lập trình mobile"));
        expenses.add(new Expense(3, "Đi siêu thị", 289000, "Mua sắm", "20/05/2026", "Hóa đơn WinMart"));
        expenses.add(new Expense(4, "Đổ xăng", 70000, "Di chuyển", "19/05/2026", "Xe máy"));
        expenses.add(new Expense(5, "Ăn trưa", 55000, "Ăn uống", "18/05/2026", "Cơm văn phòng"));
    }

    public static ArrayList<Expense> getAll() {
        return new ArrayList<>(expenses);
    }

    public static Expense findById(int id) {
        for (Expense expense : expenses) {
            if (expense.getId() == id) return expense;
        }
        return null;
    }

    public static void add(String title, double amount, String category, String date, String note) {
        expenses.add(0, new Expense(nextId++, title, amount, category, date, note));
    }

    public static void update(int id, String title, double amount, String category, String date, String note) {
        Expense expense = findById(id);
        if (expense != null) {
            expense.setTitle(title);
            expense.setAmount(amount);
            expense.setCategory(category);
            expense.setDate(date);
            expense.setNote(note);
        }
    }

    public static void delete(int id) {
        Expense found = null;
        for (Expense expense : expenses) {
            if (expense.getId() == id) {
                found = expense;
                break;
            }
        }
        if (found != null) expenses.remove(found);
    }

    public static double getTotalAmount() {
        double total = 0;
        for (Expense expense : expenses) total += expense.getAmount();
        return total;
    }
}
