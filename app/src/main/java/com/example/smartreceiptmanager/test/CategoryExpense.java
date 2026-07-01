package com.example.smartreceiptmanager.test;

// Model class chứa thông tin danh mục
public class CategoryExpense {
    private String name;
    private long amount;
    private int transactionCount;
    private int percentage;
    private String colorHex;
    private String icon;

    public CategoryExpense(String name, long amount, int transactionCount, int percentage, String colorHex, String icon) {
        this.name = name;
        this.amount = amount;
        this.transactionCount = transactionCount;
        this.percentage = percentage;
        this.colorHex = colorHex;
        this.icon = icon;
    }

    // Các hàm Getter để lấy dữ liệu
    public String getName() { return name; }
    public long getAmount() { return amount; }
    public int getTransactionCount() { return transactionCount; }
    public int getPercentage() { return percentage; }
    public String getColorHex() { return colorHex; }
    public String getIcon() { return icon; }
}