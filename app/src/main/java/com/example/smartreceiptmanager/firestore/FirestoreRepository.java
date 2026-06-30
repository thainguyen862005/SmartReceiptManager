package com.example.smartreceiptmanager.firestore;

import android.util.Log;

import com.example.smartreceiptmanager.auth.UserProfile;
import com.example.smartreceiptmanager.expense.Expense;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";

    private static final String COL_USERS        = "users";
    private static final String COL_TRANSACTIONS = "transactions";

    private final FirebaseFirestore db;

    private static FirestoreRepository instance;

    public static FirestoreRepository getInstance() {
        if (instance == null) {
            instance = new FirestoreRepository();
        }
        return instance;
    }

    private FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveUserProfile(FirebaseUser firebaseUser, OnCompleteCallback callback) {
        if (firebaseUser == null) {
            if (callback != null) callback.onFailure("FirebaseUser is null");
            return;
        }

        String uid         = firebaseUser.getUid();
        String email       = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String photoUrl    = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        // Xác định nguồn đăng nhập để điền facebook_id hoặc google_id
        String googleId   = "";
        String facebookId = "";
        if (firebaseUser.getProviderData() != null) {
            for (com.google.firebase.auth.UserInfo info : firebaseUser.getProviderData()) {
                if ("google.com".equals(info.getProviderId())) {
                    googleId = info.getUid();
                } else if ("facebook.com".equals(info.getProviderId())) {
                    facebookId = info.getUid();
                }
            }
        }

        final String finalGoogleId   = googleId;
        final String finalFacebookId = facebookId;

        // Kiểm tra user đã tồn tại chưa
        db.collection(COL_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {

                    // Sub-map profile
                    Map<String, Object> profileMap = new HashMap<>();
                    profileMap.put("full_name",      displayName);
                    profileMap.put("avatar_url",     photoUrl);
                    profileMap.put("dob",            "");
                    profileMap.put("gender",         "");
                    profileMap.put("address",        "");
                    profileMap.put("nationality",    "");
                    profileMap.put("marital_status", "");

                    // Root document
                    Map<String, Object> data = new HashMap<>();
                    data.put("email",        email);
                    data.put("phone_number", "");
                    data.put("facebook_id",  finalFacebookId);
                    data.put("google_id",    finalGoogleId);
                    data.put("profile",      profileMap);

                    if (!doc.exists()) {
                        // User mới: thêm created_at
                        data.put("created_at", System.currentTimeMillis());
                        Log.d(TAG, "Tạo user mới trên Firestore: " + uid);
                    } else {
                        Log.d(TAG, "Cập nhật user trên Firestore: " + uid);
                    }

                    db.collection(COL_USERS).document(uid)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Lưu UserProfile thành công");
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lưu UserProfile thất bại: " + e.getMessage());
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Đọc user doc thất bại: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void getUserProfile(String uid, OnProfileLoadedCallback callback) {
        db.collection(COL_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserProfile profile = doc.toObject(UserProfile.class);
                        if (callback != null) callback.onLoaded(profile);
                    } else {
                        if (callback != null) callback.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Đọc UserProfile thất bại: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void syncExpense(String uid, Expense expense, OnExpenseSyncedCallback callback) {
        if (uid == null || expense == null || expense.getId() == null) {
            if (callback != null) callback.onFailure("Dữ liệu không hợp lệ");
            return;
        }

        Map<String, Object> data = expenseToTransactionMap(expense);

        db.collection(COL_TRANSACTIONS)
                .document(uid)
                .collection("items")
                .document(expense.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Sync transaction thành công: " + expense.getId());
                    if (callback != null) callback.onSynced(expense.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sync transaction thất bại: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void syncPendingExpenses(String uid, List<Expense> pendingExpenses,
                                    OnBatchSyncCallback callback) {
        if (pendingExpenses == null || pendingExpenses.isEmpty()) {
            if (callback != null) callback.onComplete(0, 0);
            return;
        }

        int[] successCount = {0};
        int[] failCount    = {0};
        int total          = pendingExpenses.size();

        for (Expense expense : pendingExpenses) {
            syncExpense(uid, expense, new OnExpenseSyncedCallback() {
                @Override
                public void onSynced(String expenseId) {
                    successCount[0]++;
                    if (successCount[0] + failCount[0] == total) {
                        Log.d(TAG, "Batch sync xong: " + successCount[0] + " OK, " + failCount[0] + " lỗi");
                        if (callback != null) callback.onComplete(successCount[0], failCount[0]);
                    }
                }

                @Override
                public void onFailure(String error) {
                    failCount[0]++;
                    if (successCount[0] + failCount[0] == total) {
                        if (callback != null) callback.onComplete(successCount[0], failCount[0]);
                    }
                }
            });
        }
    }

    public void deleteExpense(String uid, String expenseId, OnCompleteCallback callback) {
        if (uid == null || expenseId == null) return;

        db.collection(COL_TRANSACTIONS)
                .document(uid)
                .collection("items")
                .document(expenseId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Xóa transaction Firestore OK: " + expenseId);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Xóa transaction Firestore thất bại: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }


    private Map<String, Object> expenseToTransactionMap(Expense expense) {

        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("id",   expense.getCategory() != null ? expense.getCategory() : "other");
        categoryMap.put("name", expense.getCategory() != null ? expense.getCategory() : "Khác");


        Map<String, Object> data = new HashMap<>();
        data.put("type",              "expense");
        data.put("amount",            expense.getAmount());
        data.put("wallet_id",         "default");
        data.put("note",              expense.getNote() != null ? expense.getNote() : "");
        data.put("transaction_date",  String.valueOf(expense.getDate()));
        data.put("payment_method",    "Tiền mặt");
        data.put("receipt_image_url", expense.getReceiptText() != null ? expense.getReceiptText() : "");
        data.put("created_at",        expense.getCreatedAt());
        data.put("category",          categoryMap);

        // Thêm trường để dễ query theo merchant
        data.put("merchant_name",     expense.getMerchantName() != null ? expense.getMerchantName() : "");
        data.put("is_synced",         true);

        return data;
    }


    public interface OnCompleteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnProfileLoadedCallback {
        void onLoaded(UserProfile profile);
        void onFailure(String error);
    }

    public interface OnExpenseSyncedCallback {
        void onSynced(String expenseId);
        void onFailure(String error);
    }

    public interface OnBatchSyncCallback {
        void onComplete(int successCount, int failCount);
    }
}
