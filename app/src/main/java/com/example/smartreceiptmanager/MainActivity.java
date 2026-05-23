package com.example.smartreceiptmanager;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.API_Stacistis.Statistics;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Mở Home TV2 trước
        chuyenFragment(new ExpenseFragment());

        findViewById(R.id.btnHome).setOnClickListener(v ->
                chuyenFragment(new ExpenseFragment())
        );

        findViewById(R.id.btnHistory).setOnClickListener(v ->
                Toast.makeText(this, "Đang thiết kế trang Lịch sử!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnStatistics).setOnClickListener(v ->
                chuyenFragment(new Statistics())
        );

        findViewById(R.id.btnQRScan).setOnClickListener(v ->
                Toast.makeText(this, "Mở Camera Scan hóa đơn!", Toast.LENGTH_SHORT).show()
        );
    }

    private void chuyenFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
