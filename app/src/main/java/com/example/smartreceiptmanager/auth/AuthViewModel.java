package com.example.smartreceiptmanager.auth;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartreceiptmanager.firestore.SyncManager; // Giữ nguyên import này nếu bạn có dùng
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel xử lý toàn bộ logic Auth: Email/Password, Google, Facebook.
 * Sử dụng Firebase Realtime Database với cấu trúc gốc là "User_Profiles"
 */
public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = "AuthViewModel";

    private final FirebaseAuth auth;
    // Khai báo DatabaseReference thay cho FirestoreRepository
    private final DatabaseReference databaseRef;

    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;

    public AuthViewModel(Application application) {
        super(application);
        auth = FirebaseAuth.getInstance();
        // Lấy tham chiếu gốc của Realtime Database
        databaseRef = FirebaseDatabase.getInstance().getReference();

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
                        // Chỉ đăng nhập, KHÔNG tạo lại dữ liệu
                        onLoginSuccess(user);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Đăng nhập thất bại";
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
                        // Đăng ký thành công -> Khởi tạo dữ liệu mặc định
                        onRegisterSuccess(user);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                        errorLiveData.setValue(msg);
                    }
                });
    }

    // ================================================================
    // ĐĂNG NHẬP BẰNG GOOGLE / FACEBOOK
    // ================================================================
    public void loginWithCredential(AuthCredential credential) {
        loadingLiveData.setValue(true);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    loadingLiveData.setValue(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        userLiveData.setValue(user);

                        // Kiểm tra xem đây là tài khoản mới tạo hay tài khoản cũ
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        if (isNewUser) {
                            onRegisterSuccess(user); // Tạo dữ liệu nếu là người mới
                        } else {
                            onLoginSuccess(user);    // Đăng nhập bình thường nếu là người cũ
                        }
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Xác thực mạng xã hội thất bại";
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
    // LUỒNG 1: XỬ LÝ KHI ĐĂNG KÝ TÀI KHOẢN MỚI
    // (Tạo đồng thời User, Ví, Danh mục nằm trong node User_Profiles)
    // ================================================================
    private void onRegisterSuccess(FirebaseUser user) {
        if (user == null) return;
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Người dùng mới";

        // Map khổng lồ để Update nhiều đường dẫn cùng lúc
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

        // GẮN VÀO CÁC ĐƯỜNG DẪN CÓ TIỀN TỐ "User_Profiles/"
        childUpdates.put("User_Profiles/users/" + uid, userProfileObj);
        childUpdates.put("User_Profiles/wallets/" + uid + "/wallet_default_01", defaultWallet);
        childUpdates.put("User_Profiles/categories/" + uid + "/cate_expense_01", expenseCategory);
        childUpdates.put("User_Profiles/categories/" + uid + "/cate_income_01", incomeCategory);

        // Thực thi đẩy lên Firebase
        databaseRef.updateChildren(childUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Đã khởi tạo dữ liệu mặc định thành công cho UID: " + uid);
                    // Kích hoạt đồng bộ ngoại tuyến nếu cần
                    if (getApplication() != null) {
                        try {
                            SyncManager.getInstance(getApplication()).syncPendingIfOnline();
                        } catch (Exception e) {
                            Log.e(TAG, "SyncManager error: " + e.getMessage());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tạo dữ liệu: " + e.getMessage()));
    }

    // ================================================================
    // LUỒNG 2: XỬ LÝ KHI ĐĂNG NHẬP (TÀI KHOẢN ĐÃ TỒN TẠI)
    // ================================================================
    private void onLoginSuccess(FirebaseUser user) {
        if (user == null) return;
        Log.d(TAG, "Người dùng cũ đăng nhập thành công: " + user.getUid());

        // Không tạo lại dữ liệu, chỉ đồng bộ
        if (getApplication() != null) {
            try {
                SyncManager.getInstance(getApplication()).syncPendingIfOnline();
            } catch (Exception e) {
                Log.e(TAG, "SyncManager error: " + e.getMessage());
            }
        }
    }
}