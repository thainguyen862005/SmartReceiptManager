package com.example.smartreceiptmanager;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.smartreceiptmanager.databinding.ActivityProfileBinding;
import com.example.smartreceiptmanager.utils.ThemeHelper;
import com.example.smartreceiptmanager.auth.AuthViewModel;
import com.example.smartreceiptmanager.auth.UserProfile;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import android.net.Uri;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private AuthViewModel authViewModel;

    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadAvatar(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupProfileData();
        setupThemeStatus();
        setupListeners();
    }

    private void setupProfileData() {
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                binding.tvEmail.setText(firebaseUser.getEmail());
            } else {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getUserProfileLiveData().observe(this, userProfile -> {
            if (userProfile != null && userProfile.getProfile() != null) {
                binding.tvName.setText(
                        userProfile.getProfile().getFull_name() != null && !userProfile.getProfile().getFull_name().isEmpty()
                                ? userProfile.getProfile().getFull_name() : "Người dùng Pro");
                String avatarUrl = userProfile.getProfile().getAvatar_url();
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    if (avatarUrl.startsWith("http")) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .circleCrop()
                                .into(binding.imgAvatar);
                    } else {
                        try {
                            byte[] bytes = android.util.Base64.decode(avatarUrl, android.util.Base64.DEFAULT);
                            Glide.with(this)
                                    .load(bytes)
                                    .placeholder(android.R.drawable.sym_def_app_icon)
                                    .circleCrop()
                                    .into(binding.imgAvatar);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });
    }

    private void setupThemeStatus() {
        int currentTheme = ThemeHelper.getSavedTheme(this);
        switch (currentTheme) {
            case 1:
                binding.tvCurrentTheme.setText("Chế độ Sáng");
                break;
            case 2:
                binding.tvCurrentTheme.setText("Chế độ Tối");
                break;
            default:
                binding.tvCurrentTheme.setText("Hệ thống");
                break;
        }
    }

    private void setupListeners() {
        binding.itemTheme.setOnClickListener(v -> {
            int current = ThemeHelper.getSavedTheme(this);
            int next = (current + 1) % 3;
            ThemeHelper.saveTheme(this, next);
            recreate();
        });

        binding.btnLogout.setOnClickListener(v -> authViewModel.logout());

        binding.imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.itemAccountSettings.setOnClickListener(v -> {
            String[] options;
            if (isSocialLogin()) {
                options = new String[]{"Cập nhật tên hiển thị", "Xóa tài khoản"};
            } else {
                options = new String[]{"Cập nhật tên hiển thị", "Đổi mật khẩu", "Xóa tài khoản"};
            }
            new AlertDialog.Builder(this)
                    .setTitle("Cài đặt tài khoản")
                    .setItems(options, (dialog, which) -> {
                        String selected = options[which];
                        if ("Cập nhật tên hiển thị".equals(selected)) {
                            showUpdateNameDialog();
                        } else if ("Đổi mật khẩu".equals(selected)) {
                            showResetPasswordDialog();
                        } else if ("Xóa tài khoản".equals(selected)) {
                            showDeleteAccountDialog();
                        }
                    })
                    .show();
        });

        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser == null) {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void uploadAvatar(Uri uri) {
        try {
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            if (bitmap == null) return;

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float maxRatio = 120f / Math.max(width, height);
            if (maxRatio < 1) {
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, (int) (width * maxRatio), (int) (height * maxRatio), true);
            }

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] bytes = outputStream.toByteArray();
            String base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);

            authViewModel.updateAvatar(base64Image);
            Toast.makeText(this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showUpdateNameDialog() {
        EditText input = new EditText(this);
        input.setText(binding.tvName.getText().toString());
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
                            Toast.makeText(ProfileActivity.this, "Mật khẩu mới không trùng khớp", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        authViewModel.changePassword(oldPwd, newPwd, new AuthViewModel.OnPasswordChangeListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(ProfileActivity.this, "Đã đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(ProfileActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
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