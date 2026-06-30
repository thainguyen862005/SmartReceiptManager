package com.example.smartreceiptmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.smartreceiptmanager.databinding.ActivityLoginBinding;
import com.example.smartreceiptmanager.auth.AuthViewModel;
import com.example.smartreceiptmanager.utils.ThemeHelper;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import java.util.Collections;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 1. Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 2. Cấu hình Facebook SDK Login Callback
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Đăng nhập Facebook bị hủy", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Lỗi Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        setupListeners();
        setupObservers();
    }

    private void setupListeners() {
        // Nút Đăng nhập bằng Email & Mật khẩu thông thường
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.edtEmail.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            authViewModel.login(email, password);
        });

        // Sự kiện click nút chuyển sang màn hình Đăng ký
        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Sự kiện click nút Đăng nhập Google
        binding.btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleLauncher.launch(signInIntent);
        });

        // Sự kiện click nút Đăng nhập Facebook
        binding.btnFacebookLogin.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("email"));
        });

        // Nút đổi Theme nhanh (Sáng/Tối)
        binding.btnThemeSelect.setOnClickListener(v -> {
            int current = ThemeHelper.getSavedTheme(this);
            int next = (current + 1) % 3;
            ThemeHelper.saveTheme(this, next);
            recreate(); // Áp dụng theme ngay lập tức
        });
    }

    private void setupObservers() {
        // Lắng nghe trạng thái đăng nhập từ Firebase
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish(); // Đóng LoginActivity để không quay lại được khi bấm nút Back
            }
        });

        // Lắng nghe thông báo lỗi từ hệ thống
        authViewModel.getErrorLiveData().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Bộ nhận kết quả trả về từ màn hình đăng nhập của Google
    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            authViewModel.loginWithCredential(credential);
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign In thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Chuyển đổi Access Token của Facebook thành Credential của Firebase
    private void handleFacebookToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        authViewModel.loginWithCredential(credential);
    }

    // Hàm chuyển tiếp dữ liệu kết quả Đăng nhập Facebook về cho CallbackManager xử lý
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}