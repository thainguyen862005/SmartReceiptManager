package com.example.smartreceiptmanager.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {

    public static String formatVnd(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    public static String formatAmount(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    public static double parseAmount(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }

        try {
            String cleaned = input
                    .replace(".", "")
                    .replace(",", "")
                    .replace("đ", "")
                    .replace("VNĐ", "")
                    .replace("vnd", "")
                    .trim();

            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }
}