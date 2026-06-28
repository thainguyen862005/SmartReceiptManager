package com.example.smartreceiptmanager;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.smartreceiptmanager.databinding.ActivityProfileBinding;
import com.example.smartreceiptmanager.utils.ThemeHelper;
import com.example.smartreceiptmanager.auth.AuthViewModel;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.content.SharedPreferences;
import android.content.Context;
import java.util.Calendar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import com.example.smartreceiptmanager.utils.NotificationReceiver;

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

        binding.btnEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.itemAccountSettings.setOnClickListener(v -> {
            String[] options;
            if (isSocialLogin()) {
                options = new String[]{"Cập nhật tên hiển thị", "Cài đặt ngân sách tháng", "Xóa tài khoản"};
            } else {
                options = new String[]{"Cập nhật tên hiển thị", "Đổi mật khẩu", "Cài đặt ngân sách tháng", "Xóa tài khoản"};
            }
            new AlertDialog.Builder(this)
                    .setTitle("Cài đặt tài khoản")
                    .setItems(options, (dialog, which) -> {
                        String selected = options[which];
                        if ("Cập nhật tên hiển thị".equals(selected)) {
                            showUpdateNameDialog();
                        } else if ("Đổi mật khẩu".equals(selected)) {
                            showResetPasswordDialog();
                        } else if ("Cài đặt ngân sách tháng".equals(selected)) {
                            showBudgetLimitDialog();
                        } else if ("Xóa tài khoản".equals(selected)) {
                            showDeleteAccountDialog();
                        }
                    })
                    .show();
        });

        binding.itemNotificationSettings.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("smart_receipt_prefs", MODE_PRIVATE);
            boolean[] checkedItems = {
                    prefs.getBoolean("notif_reminder", true),
                    prefs.getBoolean("notif_budget", true),
                    prefs.getBoolean("notif_report", false),
                    prefs.getBoolean("notif_sync", true)
            };
            String[] options = {"Nhắc nhở quét hóa đơn hàng ngày", "Cảnh báo vượt ngân sách chi tiêu", "Nhận báo cáo tài chính hàng tuần", "Đồng bộ hóa đám mây thành công"};
            new AlertDialog.Builder(this)
                    .setTitle("Cài đặt thông báo")
                    .setMultiChoiceItems(options, checkedItems, (d, which, isChecked) -> {
                        checkedItems[which] = isChecked;
                    })
                    .setPositiveButton("Xác nhận", (d, w) -> {
                        prefs.edit()
                                .putBoolean("notif_reminder", checkedItems[0])
                                .putBoolean("notif_budget", checkedItems[1])
                                .putBoolean("notif_report", checkedItems[2])
                                .putBoolean("notif_sync", checkedItems[3])
                                .apply();
                        updateDailyReminder(checkedItems[0]);

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                            }
                        }
                        Toast.makeText(this, "Đã lưu cài đặt thông báo", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        binding.btnNotifications.setOnClickListener(v -> {
            String[] notifications = com.example.smartreceiptmanager.utils.NotificationHelper.getNotifications(this);
            new AlertDialog.Builder(this)
                    .setTitle("Thông báo")
                    .setItems(notifications, null)
                    .setPositiveButton("Đóng", null)
                    .show();
        });

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


    private void uploadAvatar(Uri uri) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
        String uid = user.getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference avatarRef = storageRef.child("avatars/" + uid + ".jpg");
        avatarRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    avatarRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String downloadUrl = downloadUri.toString();
                        authViewModel.updateAvatar(downloadUrl);
                        Toast.makeText(ProfileActivity.this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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

    private void showBudgetLimitDialog() {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        SharedPreferences prefs = getSharedPreferences("smart_receipt_prefs", MODE_PRIVATE);
        long currentBudget = prefs.getLong("budget_limit", 10000000L);
        input.setText(String.valueOf(currentBudget));
        new AlertDialog.Builder(this)
                .setTitle("Cài đặt ngân sách tháng (VND)")
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try {
                            long newBudget = Long.parseLong(val);
                            prefs.edit().putLong("budget_limit", newBudget).apply();
                            Toast.makeText(this, "Đã cập nhật ngân sách hàng tháng", Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa tài khoản")
                .setMessage("Cảnh báo: Hành động này sẽ xóa toàn bộ dữ liệu giao dịch và tài khoản của bạn vĩnh viễn. Bạn có chắc chắn muốn tiếp tục?")
                .setPositiveButton("Xóa vĩnh viễn", (d, w) -> {
                    authViewModel.deleteAccount();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateDailyReminder(boolean enable) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager == null) return;

        if (enable) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 20);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else {
            alarmManager.cancel(pendingIntent);
        }
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