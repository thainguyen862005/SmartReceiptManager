package com.example.smartreceiptmanager.test;

import java.util.ArrayList;
import java.util.List;

// Lớp cung cấp dữ liệu giả lập
public class MockDatabase {
    public static List<CategoryExpense> getTopCategories() {
        List<CategoryExpense> list = new ArrayList<>();
        
        // Đổ data y hệt như bản thiết kế UI của bạn
        list.add(new CategoryExpense("Ăn uống", 5000000, 24, 40, "#006A4E", "🍽️"));
        list.add(new CategoryExpense("Di chuyển", 3750000, 12, 30, "#A3E4D7", "🚗"));
        list.add(new CategoryExpense("Mua sắm", 2500000, 8, 20, "#AED6F1", "🛍️"));
        list.add(new CategoryExpense("Khác", 1250000, 5, 10, "#FADBD8", "📦"));
        
        return list;
    }
}