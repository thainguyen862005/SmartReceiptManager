package com.example.smartreceiptmanager.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtils {

    public static String formatVnd(double amount) {
        return formatAmount(amount) + "đ";
    }

    public static String formatAmount(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
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
                    .replace("Đ", "")
                    .replace("VNĐ", "")
                    .replace("vnd", "")
                    .trim();

            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }
}
