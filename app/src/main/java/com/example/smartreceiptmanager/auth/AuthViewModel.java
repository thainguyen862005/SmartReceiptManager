package com.example.smartreceiptmanager.auth;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartreceiptmanager.firestore.SyncManager;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.net.Uri;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.EmailAuthProvider;

import java.util.HashMap;
import java.util.Map;

public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = "AuthViewModel";

    private final FirebaseAuth auth;
    private final DatabaseReference databaseRef;

    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;

    public AuthViewModel(Application application) {
        super(application);
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        userLiveData = new MutableLiveData<>(auth.getCurrentUser());
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>(false);
    }

    public LiveData<FirebaseUser> getUserLiveData()   { return userLiveData; }
    public LiveData<String>       getErrorLiveData()  { return errorLiveData; }
    public LiveData<Boolean>      getLoadingLiveData(){ return loadingLiveData; }

    public void login(String email, String password) {
        loadingLiveData.setValue(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loadingLiveData.setValue(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        userLiveData.setValue(user);
                        onLoginSuccess(user);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Đăng nhập thất bại";
                        errorLiveData.setValue(msg);
                    }
                });
    }


    public void register(String email, String password) {
        loadingLiveData.setValue(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();

                        onRegisterSuccess(user);
                    } else {
                        loadingLiveData.setValue(false);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                        errorLiveData.setValue(msg);
                    }
                });
    }


    public void loginWithCredential(AuthCredential credential) {
        loadingLiveData.setValue(true);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        if (isNewUser) {
                            onRegisterSuccess(user);
                        } else {
                            loadingLiveData.setValue(false);
                            userLiveData.setValue(user);
                            onLoginSuccess(user);
                        }
                    } else {
                        loadingLiveData.setValue(false);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Xác thực mạng xã hội thất bại";
                        errorLiveData.setValue(msg);
                    }
                });
    }

    public void logout() {
        auth.signOut();
        userLiveData.setValue(null);
    }

    public void updateAvatar(String newUrl) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(newUrl))
                    .build();
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = user.getUid();
                            databaseRef.child("User_Profiles/users").child(uid).child("profile").child("avatar_url").setValue(newUrl);
                            userLiveData.setValue(auth.getCurrentUser());
                        }
                    });
        }
    }

    public void updateDisplayName(String newName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = user.getUid();
                            databaseRef.child("User_Profiles/users").child(uid).child("profile").child("full_name").setValue(newName);
                            userLiveData.setValue(auth.getCurrentUser());
                        }
                    });
        }
    }

    public void changePassword(String oldPassword, String newPassword, OnPasswordChangeListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            listener.onSuccess();
                                        } else {
                                            listener.onFailure(updateTask.getException() != null ? updateTask.getException().getMessage() : "Update password failed");
                                        }
                                    });
                        } else {
                            listener.onFailure("Incorrect old password");
                        }
                    });
        } else {
            listener.onFailure("User not logged in");
        }
    }

    public interface OnPasswordChangeListener {
        void onSuccess();
        void onFailure(String error);
    }

    public void deleteAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            databaseRef.child("User_Profiles/users").child(uid).removeValue();
                            databaseRef.child("User_Profiles/wallets").child(uid).removeValue();
                            databaseRef.child("User_Profiles/categories").child(uid).removeValue();
                            userLiveData.setValue(null);
                        } else {
                            errorLiveData.setValue(task.getException() != null ? task.getException().getMessage() : "Delete account failed");
                        }
                    });
        }
    }

    private void onRegisterSuccess(FirebaseUser user) {
        if (user == null) {
            loadingLiveData.setValue(false);
            return;
        }
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Người dùng mới";

        Map<String, Object> childUpdates = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        // 1. Tạo Model User Profile
        UserProfile.Profile profileInner = new UserProfile.Profile(displayName, "");
        UserProfile userProfileObj = new UserProfile(email, "", "", "", profileInner);
        userProfileObj.setCreated_at(currentTime);

        // 2. Tạo Map cho Ví mặc định
        Map<String, Object> defaultWallet = new HashMap<>();
        defaultWallet.put("created_at", currentTime);
        defaultWallet.put("currency_code", "VND");
        defaultWallet.put("total_amount", 0);
        defaultWallet.put("wallet_name", "Ví tiền mặt");
        defaultWallet.put("wallet_type", "Tiền mặt");

        // 3. Tạo Map cho Danh mục mặc định
        Map<String, Object> expenseCategory = new HashMap<>();
        expenseCategory.put("category_type", "expense");
        expenseCategory.put("name", "Ăn uống");

        Map<String, Object> incomeCategory = new HashMap<>();
        incomeCategory.put("category_type", "income");
        incomeCategory.put("name", "Tiền lương");

        // Đưa vào các đường dẫn bắt đầu bằng "User_Profiles/"
        childUpdates.put("User_Profiles/users/" + uid, userProfileObj);
        childUpdates.put("User_Profiles/wallets/" + uid + "/wallet_default_01", defaultWallet);
        childUpdates.put("User_Profiles/categories/" + uid + "/cate_expense_01", expenseCategory);
        childUpdates.put("User_Profiles/categories/" + uid + "/cate_income_01", incomeCategory);

        // Đẩy đồng thời lên Firebase
        databaseRef.updateChildren(childUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Khởi tạo dữ liệu mặc định thành công cho UID: " + uid);
                    if (getApplication() != null) {
                        try {
                            SyncManager.getInstance(getApplication()).syncPendingIfOnline();
                        } catch (Exception e) {
                            Log.e(TAG, "SyncManager error: " + e.getMessage());
                        }
                    }
                    loadingLiveData.setValue(false);
                    userLiveData.setValue(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tạo dữ liệu ban đầu: " + e.getMessage());
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Lỗi thiết lập dữ liệu ban đầu: " + e.getMessage());
                });
    }

    private void onLoginSuccess(FirebaseUser user) {
        if (user == null) return;
        Log.d(TAG, "Người dùng cũ đăng nhập thành công: " + user.getUid());

        if (getApplication() != null) {
            try {
                SyncManager.getInstance(getApplication()).syncPendingIfOnline();
            } catch (Exception e) {
                Log.e(TAG, "SyncManager error: " + e.getMessage());
            }
        }
    }
}