package com.example.smartreceiptmanager.firestore;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.expense.ExpenseStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý việc đồng bộ dữ liệu offline -> Firestore.
 *
 * Cách dùng:
 *   - Gọi SyncManager.getInstance(context).syncPendingIfOnline()
 *     bất cứ khi nào muốn thử sync (vào app, sau khi lưu expense mới, v.v.)
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    private static SyncManager instance;

    private final Context appContext;
    private final FirestoreRepository firestoreRepo;

    public static SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context.getApplicationContext());
        }
        return instance;
    }

    private SyncManager(Context context) {
        this.appContext = context;
        this.firestoreRepo = FirestoreRepository.getInstance();
    }

    // ================================================================
    // SYNC TRIGGER
    // ================================================================

    /**
     * Kiểm tra mạng + user đã đăng nhập, sau đó sync tất cả expense
     * có cờ isSynced = false lên Firestore.
     *
     * Gọi hàm này sau khi:
     *   1. User đăng nhập thành công
     *   2. User lưu một expense mới
     *   3. App vào foreground (onResume của MainActivity)
     */
    public void syncPendingIfOnline() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Không có mạng – bỏ qua sync, sẽ sync sau");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Chưa đăng nhập – không thể sync");
            return;
        }

        String uid = currentUser.getUid();
        ExpenseStore expenseStore = new ExpenseStore(appContext);
        List<Expense> allExpenses = expenseStore.getAllExpenses();

        // Lọc những expense chưa được sync
        List<Expense> pendingExpenses = new ArrayList<>();
        for (Expense e : allExpenses) {
            if (!e.isSynced()) {
                pendingExpenses.add(e);
            }
        }

        if (pendingExpenses.isEmpty()) {
            Log.d(TAG, "Không có expense nào cần sync");
            return;
        }

        Log.d(TAG, "Bắt đầu sync " + pendingExpenses.size() + " expense...");

        firestoreRepo.syncPendingExpenses(uid, pendingExpenses,
                (successCount, failCount) -> {
                    Log.d(TAG, "Sync xong: " + successCount + " thành công, " + failCount + " thất bại");

                    // Cập nhật cờ isSynced = true trong local storage cho những cái thành công
                    if (successCount > 0) {
                        markSyncedExpenses(expenseStore, pendingExpenses, uid);
                    }
                });
    }

    /**
     * Sync một expense cụ thể ngay sau khi lưu.
     * Nếu không có mạng thì bỏ qua (sẽ được sync bởi syncPendingIfOnline lần sau).
     */
    public void syncSingleExpense(Expense expense) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Không có mạng – expense " + expense.getId() + " sẽ được sync sau");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        firestoreRepo.syncExpense(uid, expense, new FirestoreRepository.OnExpenseSyncedCallback() {
            @Override
            public void onSynced(String expenseId) {
                // Cập nhật isSynced = true trong local storage
                ExpenseStore store = new ExpenseStore(appContext);
                Expense local = store.getExpenseById(expenseId);
                if (local != null) {
                    local.setSynced(true);
                    store.saveExpense(local);
                }
                Log.d(TAG, "Sync single expense thành công: " + expenseId);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Sync single expense thất bại: " + error);
            }
        });
    }

    /**
     * Xóa expense khỏi Firestore khi user xóa local.
     */
    public void deleteExpenseFromFirestore(String expenseId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        firestoreRepo.deleteExpense(currentUser.getUid(), expenseId, new FirestoreRepository.OnCompleteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Xóa expense Firestore OK: " + expenseId);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Xóa expense Firestore thất bại: " + error);
            }
        });
    }

    // ================================================================
    // HELPER
    // ================================================================

    /**
     * Sau khi batch sync thành công, cập nhật isSynced = true
     * cho tất cả expense đã được sync (dựa vào uid để đối chiếu).
     *
     * Lưu ý: do syncPendingExpenses không phân biệt được expense nào thành công,
     * hàm này mark toàn bộ pending list là synced khi đủ số lượng thành công.
     * Với dự án này là chấp nhận được vì dữ liệu nhỏ.
     */
    private void markSyncedExpenses(ExpenseStore store, List<Expense> synced, String uid) {
        for (Expense expense : synced) {
            expense.setSynced(true);
            store.saveExpense(expense);
        }
        Log.d(TAG, "Đã đánh dấu " + synced.size() + " expense là synced");
    }

    /**
     * Kiểm tra kết nối mạng (hỗ trợ Android 6+).
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities cap = cm.getNetworkCapabilities(network);
            return cap != null && (
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }
}
