package com.example.smartreceiptmanager;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.expense.ExpenseListFragment;
import com.example.smartreceiptmanager.home.HomeFragment;
import com.example.smartreceiptmanager.scanbill.ScanBillFragment;
import com.example.smartreceiptmanager.expense.AddExpenseFragment;
import com.example.smartreceiptmanager.firestore.SyncManager;
import com.example.smartreceiptmanager.statistics.StatisticsFragment;

import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {

    private static final String TAB_HOME = "HOME";
    private static final String TAB_HISTORY = "HISTORY";
    private static final String TAB_SCAN = "SCAN";
    private static final String TAB_STATISTICS = "STATISTICS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hiện tại main Page
        chuyenFragment(new HomeFragment());
        setActiveTab(TAB_HOME);

        // Bắt sự kiện các nút theo phong cách ngắn gọn (Inline) của bạn
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            chuyenFragment(new HomeFragment());
            setActiveTab(TAB_HOME);
        });

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            chuyenFragment(new ExpenseListFragment());
            setActiveTab(TAB_HISTORY);
        });

        // === ĐÃ BỔ SUNG: Bắt sự kiện click nút Cộng (+) ở đây ===
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddExpenseFragment())
                    .addToBackStack(null) // Thêm vào BackStack để hàm lắng nghe ở dưới bắt được sự kiện và tự ẩn Bottom Nav
                    .commit();
        });

        findViewById(R.id.btnStatistics).setOnClickListener(v -> {
            chuyenFragment(new StatisticsFragment());
            setActiveTab(TAB_STATISTICS);
        });

        findViewById(R.id.btnQRScan).setOnClickListener(v -> {
            chuyenFragment(new ScanBillFragment());
            setActiveTab(TAB_SCAN);
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            View bottomNav = findViewById(R.id.custom_bottom_nav);

            if (current instanceof com.example.smartreceiptmanager.expense.AddExpenseFragment
                    || current instanceof com.example.smartreceiptmanager.expense.ExpenseDetailFragment) {
                bottomNav.setVisibility(View.GONE);
            } else {
                bottomNav.setVisibility(View.VISIBLE);
            }

            if (current instanceof com.example.smartreceiptmanager.home.HomeFragment) {
                setActiveTab(TAB_HOME);
            } else if (current instanceof com.example.smartreceiptmanager.expense.ExpenseListFragment) {
                setActiveTab(TAB_HISTORY);
            } else if (current instanceof com.example.smartreceiptmanager.scanbill.ScanBillFragment) {
                setActiveTab(TAB_SCAN);
            } else if (current instanceof StatisticsFragment) {
                setActiveTab(TAB_STATISTICS);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mỗi khi app vào foreground, thử sync các expense chưa được đồng bộ
        SyncManager.getInstance(this).syncPendingIfOnline();
    }

    private void chuyenFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void setActiveTab(String selectedTab) {
        View layoutHome = findViewById(R.id.layoutHomeContent);
        View layoutHistory = findViewById(R.id.layoutHistoryContent);
        View layoutScan = findViewById(R.id.layoutScanContent);
        View layoutStatistics = findViewById(R.id.layoutStatisticsContent);

        ImageView iconHome = findViewById(R.id.iconHome);
        ImageView iconHistory = findViewById(R.id.iconHistory);
        ImageView iconQRScan = findViewById(R.id.iconQRScan);
        ImageView iconStatistics = findViewById(R.id.iconStatistics);

        TextView txtHome = findViewById(R.id.txtHome);
        TextView txtHistory = findViewById(R.id.txtHistory);
        TextView txtQRScan = findViewById(R.id.txtQRScan);
        TextView txtStatistics = findViewById(R.id.txtStatistics);

        int activeColor = ContextCompat.getColor(this, R.color.primary_green);
        int normalColor = ContextCompat.getColor(this, R.color.text_secondary);

        layoutHome.setBackgroundResource(R.drawable.bg_bottom_nav_normal);
        layoutHistory.setBackgroundResource(R.drawable.bg_bottom_nav_normal);
        layoutScan.setBackgroundResource(R.drawable.bg_bottom_nav_normal);
        layoutStatistics.setBackgroundResource(R.drawable.bg_bottom_nav_normal);

        iconHome.setImageTintList(ColorStateList.valueOf(normalColor));
        iconHistory.setImageTintList(ColorStateList.valueOf(normalColor));
        iconQRScan.setImageTintList(ColorStateList.valueOf(normalColor));
        iconStatistics.setImageTintList(ColorStateList.valueOf(normalColor));

        txtHome.setTextColor(normalColor);
        txtHistory.setTextColor(normalColor);
        txtQRScan.setTextColor(normalColor);
        txtStatistics.setTextColor(normalColor);

        if (TAB_HOME.equals(selectedTab)) {
            layoutHome.setBackgroundResource(R.drawable.bg_bottom_nav_active);
            iconHome.setImageTintList(ColorStateList.valueOf(activeColor));
            txtHome.setTextColor(activeColor);

        } else if (TAB_HISTORY.equals(selectedTab)) {
            layoutHistory.setBackgroundResource(R.drawable.bg_bottom_nav_active);
            iconHistory.setImageTintList(ColorStateList.valueOf(activeColor));
            txtHistory.setTextColor(activeColor);

        } else if (TAB_SCAN.equals(selectedTab)) {
            layoutScan.setBackgroundResource(R.drawable.bg_bottom_nav_active);
            iconQRScan.setImageTintList(ColorStateList.valueOf(activeColor));
            txtQRScan.setTextColor(activeColor);

        } else if (TAB_STATISTICS.equals(selectedTab)) {
            layoutStatistics.setBackgroundResource(R.drawable.bg_bottom_nav_active);
            iconStatistics.setImageTintList(ColorStateList.valueOf(activeColor));
            txtStatistics.setTextColor(activeColor);
        }
    }

}