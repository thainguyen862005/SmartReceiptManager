package com.example.smartreceiptmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.smartreceiptmanager.databinding.ActivityAccountSettingsBinding;
import com.example.smartreceiptmanager.auth.AuthViewModel;

public class AccountSettingsActivity extends AppCompatActivity {

    private ActivityAccountSettingsBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupViewVisibility();
        setupListeners();
        setupObservers();
    }

    private void setupViewVisibility() {
        if (isSocialLogin()) {
            binding.itemChangePassword.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.itemUpdateName.setOnClickListener(v -> showUpdateNameDialog());
        binding.itemChangePassword.setOnClickListener(v -> showResetPasswordDialog());
        binding.itemDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void setupObservers() {
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser == null) {
                finish();
            }
        });
    }

    private void showUpdateNameDialog() {
        EditText input = new EditText(this);
        authViewModel.getUserProfileLiveData().observe(this, userProfile -> {
            if (userProfile != null && userProfile.getProfile() != null && input.getText().toString().isEmpty()) {
                input.setText(userProfile.getProfile().getFull_name());
            }
        });
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật tên hiển thị")
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        authViewModel.updateDisplayName(name);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showResetPasswordDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText oldPasswordInput = new EditText(this);
        oldPasswordInput.setHint("Mật khẩu hiện tại");
        oldPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(oldPasswordInput);

        EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("Mật khẩu mới");
        newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setHint("Xác nhận mật khẩu mới");
        confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmPasswordInput);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(layout)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    String oldPwd = oldPasswordInput.getText().toString().trim();
                    String newPwd = newPasswordInput.getText().toString().trim();
                    String confirmPwd = confirmPasswordInput.getText().toString().trim();
                    if (!oldPwd.isEmpty() && !newPwd.isEmpty() && !confirmPwd.isEmpty()) {
                        if (!newPwd.equals(confirmPwd)) {
                            Toast.makeText(AccountSettingsActivity.this, "Mật khẩu mới không trùng khớp", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        authViewModel.changePassword(oldPwd, newPwd, new AuthViewModel.OnPasswordChangeListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(AccountSettingsActivity.this, "Đã đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(AccountSettingsActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa tài khoản")
                .setMessage("Cảnh báo: Hành động này sẽ xóa toàn bộ dữ liệu giao dịch và tài khoản của bạn vĩnh viễn. Bạn có chắc chắn muốn tiếp tục?")
                .setPositiveButton("Xóa vĩnh viễn", (d, w) -> authViewModel.deleteAccount())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private boolean isSocialLogin() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();
                if ("google.com".equals(providerId) || "facebook.com".equals(providerId)) {
                    return true;
                }
            }
        }
        return false;
    }
}
