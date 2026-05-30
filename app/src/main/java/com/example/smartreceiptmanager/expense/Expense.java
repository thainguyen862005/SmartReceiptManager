package com.example.smartreceiptmanager.expense;

public class Expense {
    private String id;
    private String merchantName;
    private double amount;
    private String category;
    private long date;
    private String note;
    private String receiptText;
    private boolean synced;
    private long createdAt;
    private long updatedAt;

    public Expense() {
    }

    public Expense(String id, String merchantName, double amount, String category, long date,
                   String note, String receiptText, boolean synced, long createdAt, long updatedAt) {
        this.id = id;
        this.merchantName = merchantName;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
        this.receiptText = receiptText;
        this.synced = synced;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReceiptText() {
        return receiptText;
    }

    public void setReceiptText(String receiptText) {
        this.receiptText = receiptText;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
