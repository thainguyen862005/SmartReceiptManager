package com.example.smartreceiptmanager.firestore;

import android.util.Log;

import com.example.smartreceiptmanager.auth.UserProfile;
import com.example.smartreceiptmanager.expense.Expense;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý toàn bộ tương tác với Firebase Firestore.
 *
 * Cấu trúc dữ liệu trên Firestore:
 *   users/
 *     {uid}/
 *       uid: String
 *       email: String
 *       displayName: String
 *       photoUrl: String
 *       createdAt: Long
 *       updatedAt: Long
 *       expenses/
 *         {expenseId}/
 *           id: String
 *           userId: String
 *           merchantName: String
 *           amount: Double
 *           category: String
 *           date: Long
 *           note: String
 *           receiptText: String
 *           isSynced: Boolean (luôn true trên Firestore)
 *           createdAt: Long
 *           updatedAt: Long
 */
public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";

    // Tên collection
    private static final String COLLECTION_USERS    = "users";
    private static final String COLLECTION_EXPENSES = "expenses";

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
    // USER PROFILE
    // ================================================================

    /**
     * Lưu / cập nhật thông tin user lên Firestore sau khi đăng nhập thành công.
     * Dùng SetOptions.merge() để không ghi đè createdAt nếu user đã tồn tại.
     */
    public void saveUserProfile(FirebaseUser firebaseUser, OnCompleteCallback callback) {
        if (firebaseUser == null) {
            if (callback != null) callback.onFailure("FirebaseUser is null");
            return;
        }

        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        // Kiểm tra xem user đã tồn tại chưa để giữ nguyên createdAt
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("email", email);
                    data.put("displayName", displayName);
                    data.put("photoUrl", photoUrl);
                    data.put("updatedAt", System.currentTimeMillis());

                    // Nếu user mới thì thêm createdAt
                    if (!doc.exists()) {
                        data.put("createdAt", System.currentTimeMillis());
                        Log.d(TAG, "Tạo user profile mới: " + uid);
                    } else {
                        Log.d(TAG, "Cập nhật user profile: " + uid);
                    }

                    db.collection(COLLECTION_USERS).document(uid)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Lưu user profile thành công");
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lưu user profile thất bại: " + e.getMessage());
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Không đọc được user doc: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Đọc thông tin UserProfile từ Firestore theo UID.
     */
    public void getUserProfile(String uid, OnProfileLoadedCallback callback) {
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserProfile profile = doc.toObject(UserProfile.class);
                        if (callback != null) callback.onLoaded(profile);
                    } else {
                        if (callback != null) callback.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Đọc user profile thất bại: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // ================================================================
    // EXPENSE SYNC
    // ================================================================

    /**
     * Sync MỘT expense lên Firestore dưới path:
     *   users/{uid}/expenses/{expenseId}
     *
     * Sau khi sync thành công, gọi onSynced(expenseId) để caller
     * cập nhật cờ isSynced = true trong local storage.
     */
    public void syncExpense(String uid, Expense expense, OnExpenseSyncedCallback callback) {
        if (uid == null || expense == null || expense.getId() == null) {
            if (callback != null) callback.onFailure("Dữ liệu không hợp lệ");
            return;
        }

        Map<String, Object> data = expenseToMap(uid, expense);

        db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_EXPENSES)
                .document(expense.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Sync expense thành công: " + expense.getId());
                    if (callback != null) callback.onSynced(expense.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sync expense thất bại: " + expense.getId() + " - " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Sync NHIỀU expense cùng lúc (dùng khi có mạng trở lại).
     * Gọi syncExpense() tuần tự cho mỗi expense.
     */
    public void syncPendingExpenses(String uid, List<Expense> pendingExpenses,
                                    OnBatchSyncCallback callback) {
        if (pendingExpenses == null || pendingExpenses.isEmpty()) {
            if (callback != null) callback.onComplete(0, 0);
            return;
        }

        int[] successCount = {0};
        int[] failCount = {0};
        int total = pendingExpenses.size();

        for (Expense expense : pendingExpenses) {
            syncExpense(uid, expense, new OnExpenseSyncedCallback() {
                @Override
                public void onSynced(String expenseId) {
                    successCount[0]++;
                    if (successCount[0] + failCount[0] == total) {
                        Log.d(TAG, "Batch sync hoàn tất: " + successCount[0] + " thành công, "
                                + failCount[0] + " thất bại");
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
     * Xóa expense khỏi Firestore khi user xóa local.
     */
    public void deleteExpense(String uid, String expenseId, OnCompleteCallback callback) {
        if (uid == null || expenseId == null) return;

        db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_EXPENSES)
                .document(expenseId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Xóa expense Firestore thành công: " + expenseId);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Xóa expense Firestore thất bại: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // ================================================================
    // HELPER
    // ================================================================

    /**
     * Chuyển đối tượng Expense thành Map để ghi lên Firestore.
     * Thêm userId để dễ query sau này.
     */
    private Map<String, Object> expenseToMap(String uid, Expense expense) {
        Map<String, Object> data = new HashMap<>();
        data.put("id",           expense.getId());
        data.put("userId",       uid);
        data.put("merchantName", expense.getMerchantName() != null ? expense.getMerchantName() : "");
        data.put("amount",       expense.getAmount());
        data.put("category",     expense.getCategory() != null ? expense.getCategory() : "Khác");
        data.put("date",         expense.getDate());
        data.put("note",         expense.getNote() != null ? expense.getNote() : "");
        data.put("receiptText",  expense.getReceiptText() != null ? expense.getReceiptText() : "");
        data.put("isSynced",     true);   // Trên Firestore luôn là true
        data.put("createdAt",    expense.getCreatedAt());
        data.put("updatedAt",    System.currentTimeMillis());
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
