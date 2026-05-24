package com.example.smartreceiptmanager;

public class Expense {
    private int id;
    private String title;
    private double amount;
    private String category;
    private String date;
    private String note;

    public Expense(int id, String title, double amount, String category, String date, String note) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getNote() { return note; }

    public void setTitle(String title) { this.title = title; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCategory(String category) { this.category = category; }
    public void setDate(String date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
}
