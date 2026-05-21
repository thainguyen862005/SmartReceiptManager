package com.example.smartreceiptmanager;

import android.os.Bundle;
import android.widget.Toast; // Thêm import này để xài Toast thông báo
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.smartreceiptmanager.API_Stacistis.Statistics;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hiện tại chỉ mới có trang Thống kê nên tạm thời mở app lên sẽ load trang này trước
        chuyenFragment(new Statistics());

        // Bắt sự kiện các nút theo phong cách ngắn gọn (Inline) của bạn
        findViewById(R.id.btnHome).setOnClickListener(v -> Toast.makeText(this, "Đang thiết kế trang Chủ!", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnHistory).setOnClickListener(v -> Toast.makeText(this, "Đang thiết kế trang Lịch sử!", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnStatistics).setOnClickListener(v -> chuyenFragment(new Statistics()));
        findViewById(R.id.btnQRScan).setOnClickListener(v -> Toast.makeText(this, "Mở Camera Scan hóa đơn!", Toast.LENGTH_SHORT).show());
    }

    private void chuyenFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}