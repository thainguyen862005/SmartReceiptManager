package com.example.smartreceiptmanager;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.smartreceiptmanager.databinding.ActivityProfileBinding;
import com.example.smartreceiptmanager.utils.ThemeHelper;
import com.example.smartreceiptmanager.auth.AuthViewModel;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private AuthViewModel authViewModel;

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
                // Hiển thị thông tin động lấy từ Firebase
                binding.tvName.setText(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng Pro");
                binding.tvEmail.setText(firebaseUser.getEmail());

                if (firebaseUser.getPhotoUrl() != null) {
                    Glide.with(this)
                            .load(firebaseUser.getPhotoUrl())
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .circleCrop()
                            .into(binding.imgAvatar);
                }
            } else {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
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

        // Lắng nghe sự kiện đăng xuất: khi user = null thì chuyển về LoginActivity
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser == null) {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}