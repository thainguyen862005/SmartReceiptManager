package com.example.smartreceiptmanager;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.API_Stacistis.Statistics;
import com.example.smartreceiptmanager.scanbill.ScanBillFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                chuyenFragment(new ScanBillFragment())
        );
    }

    private void chuyenFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}