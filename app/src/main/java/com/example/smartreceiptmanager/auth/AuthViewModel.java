package com.example.smartreceiptmanager.auth;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartreceiptmanager.firestore.FirestoreRepository;
import com.example.smartreceiptmanager.firestore.SyncManager;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel xử lý toàn bộ logic Auth: Email/Password, Google, Facebook.
 * Sau khi đăng nhập thành công:
 *   1. Lưu / cập nhật UserProfile lên Firestore (users/{uid})
 *   2. Trigger sync các expense còn pending lên Firestore
 */
public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = "AuthViewModel";

    private final FirebaseAuth auth;
    private final FirestoreRepository firestoreRepo;

    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<String> errorLiveData;
    // Loading state để UI có thể hiển thị spinner nếu cần
    private final MutableLiveData<Boolean> loadingLiveData;

    public AuthViewModel(Application application) {
        super(application);
        auth = FirebaseAuth.getInstance();
        firestoreRepo = FirestoreRepository.getInstance();
        userLiveData = new MutableLiveData<>(auth.getCurrentUser());
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>(false);
    }

    public LiveData<FirebaseUser> getUserLiveData()   { return userLiveData; }
    public LiveData<String>       getErrorLiveData()  { return errorLiveData; }
    public LiveData<Boolean>      getLoadingLiveData(){ return loadingLiveData; }

    // ================================================================
    // ĐĂNG NHẬP BẰNG EMAIL & PASSWORD
    // ================================================================

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
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Đăng nhập thất bại";
                        errorLiveData.setValue(msg);
                    }
                });
    }

    // ================================================================
    // ĐĂNG KÝ BẰNG EMAIL & PASSWORD
    // ================================================================

    public void register(String email, String password) {
        loadingLiveData.setValue(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loadingLiveData.setValue(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        userLiveData.setValue(user);
                        onLoginSuccess(user);
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Đăng ký thất bại";
                        errorLiveData.setValue(msg);
                    }
                });
    }

    // ================================================================
    // ĐĂNG NHẬP BẰNG GOOGLE / FACEBOOK (Social Credential)
    // ================================================================

    public void loginWithCredential(AuthCredential credential) {
        loadingLiveData.setValue(true);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    loadingLiveData.setValue(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        userLiveData.setValue(user);
                        onLoginSuccess(user);
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Xác thực mạng xã hội thất bại";
                        errorLiveData.setValue(msg);
                    }
                });
    }

    // ================================================================
    // ĐĂNG XUẤT
    // ================================================================

    public void logout() {
        auth.signOut();
        userLiveData.setValue(null);
    }

    // ================================================================
    // SAU KHI ĐĂNG NHẬP THÀNH CÔNG
    // ================================================================

    /**
     * Hàm này được gọi ngay sau khi bất kỳ phương thức đăng nhập nào thành công.
     * 1. Lưu/cập nhật thông tin user lên Firestore (collection: users/{uid})
     * 2. Trigger sync các expense pending lên Firestore
     */
    private void onLoginSuccess(FirebaseUser user) {
        if (user == null) return;

        // 1. Lưu UserProfile lên Firestore
        firestoreRepo.saveUserProfile(user, new FirestoreRepository.OnCompleteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "UserProfile đã lưu lên Firestore: " + user.getUid());
            }

            @Override
            public void onFailure(String error) {
                // Không block luồng chính, chỉ log lỗi
                Log.e(TAG, "Lưu UserProfile thất bại: " + error);
            }
        });

        // 2. Sync các expense offline lên Firestore
        SyncManager.getInstance(getApplication()).syncPendingIfOnline();
    }
}