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

/**
 * Quản lý toàn bộ tương tác với Firebase Firestore.
 *
 * Cấu trúc dữ liệu trên Firestore (align theo schema Việt thiết kế):
 *
 *   users/{uid}/
 *     email: String
 *     phone_number: String
 *     facebook_id: String
 *     google_id: String
 *     created_at: Long (timestamp)
 *     profile/
 *       full_name: String
 *       dob: String
 *       gender: String
 *       address: String
 *       nationality: String
 *       marital_status: String
 *       avatar_url: String
 *
 *   transactions/{uid}/{transactionId}/
 *     type: "expense" | "income"
 *     amount: Double
 *     wallet_id: String
 *     note: String
 *     transaction_date: String
 *     payment_method: String
 *     receipt_image_url: String
 *     created_at: Long (timestamp)
 *     category/
 *       id: String
 *       name: String
 */
public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";

    // Tên collection – align theo schema Việt
    private static final String COL_USERS        = "users";
    private static final String COL_TRANSACTIONS = "transactions";  // Đổi từ "expenses" → "transactions"

    private final FirebaseFirestore db;

    // -------- Singleton --------
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

    // ================================================================
    // USER PROFILE – lưu theo schema Việt
    // ================================================================

    /**
     * Lưu/cập nhật thông tin user lên Firestore sau khi đăng nhập thành công.
     * Field names theo đúng schema Việt: email, phone_number, facebook_id, google_id,
     * created_at, và sub-map profile { full_name, avatar_url, ... }
     */
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

        // Kiểm tra user đã tồn tại chưa (để giữ created_at)
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

    /**
     * Đọc UserProfile từ Firestore theo uid.
     */
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

    // ================================================================
    // TRANSACTIONS – align theo schema Việt (đổi tên từ expenses)
    // ================================================================

    /**
     * Sync một Expense lên Firestore dưới path: transactions/{uid}/{expenseId}
     * Fields được map theo schema Việt: type, amount, wallet_id, note,
     * transaction_date, payment_method, receipt_image_url, created_at, category{}
     */
    public void syncExpense(String uid, Expense expense, OnExpenseSyncedCallback callback) {
        if (uid == null || expense == null || expense.getId() == null) {
            if (callback != null) callback.onFailure("Dữ liệu không hợp lệ");
            return;
        }

        Map<String, Object> data = expenseToTransactionMap(expense);

        db.collection(COL_TRANSACTIONS)
                .document(uid)
                .collection("items")          // transactions/{uid}/items/{expenseId}
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

    /**
     * Sync nhiều expense cùng lúc (batch sync khi có mạng trở lại).
     */
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

    /**
     * Xóa transaction khỏi Firestore khi user xóa local.
     */
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

    // ================================================================
    // HELPER – chuyển Expense sang Map theo schema Việt
    // ================================================================

    private Map<String, Object> expenseToTransactionMap(Expense expense) {
        // Sub-map category (denormalization theo schema Việt)
        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("id",   expense.getCategory() != null ? expense.getCategory() : "other");
        categoryMap.put("name", expense.getCategory() != null ? expense.getCategory() : "Khác");

        // Root map – đúng theo schema Việt (Bảng 4: transactions)
        Map<String, Object> data = new HashMap<>();
        data.put("type",              "expense");                 // "expense" hoặc "income"
        data.put("amount",            expense.getAmount());
        data.put("wallet_id",         "default");                 // Mặc định ví chính
        data.put("note",              expense.getNote() != null ? expense.getNote() : "");
        data.put("transaction_date",  String.valueOf(expense.getDate()));
        data.put("payment_method",    "Tiền mặt");               // Default
        data.put("receipt_image_url", expense.getReceiptText() != null ? expense.getReceiptText() : "");
        data.put("created_at",        expense.getCreatedAt());
        data.put("category",          categoryMap);

        // Thêm trường để dễ query theo merchant
        data.put("merchant_name",     expense.getMerchantName() != null ? expense.getMerchantName() : "");
        data.put("is_synced",         true);

        return data;
    }

    // ================================================================
    // CALLBACK INTERFACES
    // ================================================================

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
