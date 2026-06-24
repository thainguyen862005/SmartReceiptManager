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

                    if (successCount > 0) {
                        markSyncedExpenses(expenseStore, pendingExpenses, uid);
                    }
                });
    }

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

    private void markSyncedExpenses(ExpenseStore store, List<Expense> synced, String uid) {
        for (Expense expense : synced) {
            expense.setSynced(true);
            store.saveExpense(expense);
        }
        Log.d(TAG, "Đã đánh dấu " + synced.size() + " expense là synced");
    }

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
