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
        binding.btnBack.setOnClickListener(v -> finish());

        binding.itemTheme.setOnClickListener(v -> {
            int current = ThemeHelper.getSavedTheme(this);
            int next = (current + 1) % 3;
            ThemeHelper.saveTheme(this, next);
            recreate();
        });

        binding.btnLogout.setOnClickListener(v -> authViewModel.logout());

        binding.imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.itemAccountSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AccountSettingsActivity.class);
            startActivity(intent);
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
}