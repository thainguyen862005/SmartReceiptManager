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
            Log.d(TAG, "No network, skip sync");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No signed-in user, skip sync");
            return;
        }

        String uid = currentUser.getUid();
        ExpenseStore expenseStore = new ExpenseStore(appContext);
        List<Expense> allExpenses = expenseStore.getAllExpenses();
        List<String> pendingDeletedIds = expenseStore.getPendingDeletedExpenseIds();

        if (!pendingDeletedIds.isEmpty()) {
            syncPendingDeletes(uid, expenseStore, pendingDeletedIds);
        }

        List<Expense> pendingExpenses = new ArrayList<>();
        for (Expense expense : allExpenses) {
            if (!expense.isSynced()) {
                pendingExpenses.add(expense);
            }
        }

        if (pendingExpenses.isEmpty()) {
            Log.d(TAG, pendingDeletedIds.isEmpty()
                    ? "No expenses need sync"
                    : "Only pending deletes need sync");
            return;
        }

        Log.d(TAG, "Start syncing " + pendingExpenses.size() + " expenses");

        firestoreRepo.syncPendingExpenses(uid, pendingExpenses,
                (successCount, failCount) -> {
                    Log.d(TAG, "Sync completed: " + successCount + " success, " + failCount + " failed");

                    if (successCount > 0) {
                        markSyncedExpenses(expenseStore, pendingExpenses, uid);
                    }
                });
    }

    public void syncSingleExpense(Expense expense) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network, expense will sync later: " + expense.getId());
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        firestoreRepo.syncExpense(uid, expense, new FirestoreRepository.OnExpenseSyncedCallback() {
            @Override
            public void onSynced(String expenseId) {
                ExpenseStore store = new ExpenseStore(appContext);
                Expense local = store.getExpenseById(expenseId);
                if (local != null) {
                    local.setSynced(true);
                    store.saveExpense(local);
                }
                Log.d(TAG, "Single expense synced: " + expenseId);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Single expense sync failed: " + error);
            }
        });
    }

    public void deleteExpenseFromFirestore(String expenseId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        firestoreRepo.deleteExpense(currentUser.getUid(), expenseId, new FirestoreRepository.OnCompleteCallback() {
            @Override
            public void onSuccess() {
                new ExpenseStore(appContext).markDeleteSynced(expenseId);
                Log.d(TAG, "Firestore expense deleted: " + expenseId);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Firestore expense delete failed: " + error);
            }
        });
    }

    private void markSyncedExpenses(ExpenseStore store, List<Expense> synced, String uid) {
        for (Expense expense : synced) {
            expense.setSynced(true);
            store.saveExpense(expense);
        }
        Log.d(TAG, "Marked " + synced.size() + " expenses as synced");
    }

    private void syncPendingDeletes(String uid, ExpenseStore store, List<String> pendingDeletedIds) {
        Log.d(TAG, "Start syncing " + pendingDeletedIds.size() + " pending deletes");

        for (String expenseId : pendingDeletedIds) {
            firestoreRepo.deleteExpense(uid, expenseId, new FirestoreRepository.OnCompleteCallback() {
                @Override
                public void onSuccess() {
                    store.markDeleteSynced(expenseId);
                    Log.d(TAG, "Pending delete synced: " + expenseId);
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Pending delete sync failed: " + expenseId + " - " + error);
                }
            });
        }
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
