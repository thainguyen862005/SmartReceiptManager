package com.example.smartreceiptmanager.utils;

public class Constants {

    // API chạy local trên máy tính, Android Emulator sẽ gọi qua 10.0.2.2
    public static final String BASE_URL = "http://10.0.2.2:3000/";

    // Firebase / Firestore collection names
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_EXPENSES = "expenses";
    public static final String COLLECTION_CATEGORIES = "categories";

    // Default categories
    public static final String CATEGORY_FOOD = "Ăn uống";
    public static final String CATEGORY_TRANSPORT = "Di chuyển";
    public static final String CATEGORY_SHOPPING = "Mua sắm";
    public static final String CATEGORY_HEALTH = "Sức khỏe";
    public static final String CATEGORY_ENTERTAINMENT = "Giải trí";
    public static final String CATEGORY_EDUCATION = "Học tập";
    public static final String CATEGORY_OTHER = "Khác";

    // Sync status
    public static final String SYNCED = "SYNCED";
    public static final String PENDING = "PENDING";
    public static final String FAILED = "FAILED";
}