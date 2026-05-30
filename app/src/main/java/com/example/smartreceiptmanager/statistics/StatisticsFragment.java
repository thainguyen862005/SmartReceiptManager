package com.example.smartreceiptmanager.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;

public class StatisticsFragment extends Fragment {

    public StatisticsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout container = view.findViewById(R.id.layoutTopCategoriesContainer);
        container.removeAllViews();

        addTopCategory(container, "🍽️", "Ăn uống", "24 giao dịch", "5,000,000 đ", 90);
        addTopCategory(container, "🚗", "Di chuyển", "15 giao dịch", "3,750,000 đ", 68);
        addTopCategory(container, "🛍", "Mua sắm", "8 giao dịch", "2,500,000 đ", 45);
    }

    private void addTopCategory(
            LinearLayout container,
            String icon,
            String name,
            String count,
            String amount,
            int progress
    ) {
        View item = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_top_category, container, false);

        TextView tvIcon = item.findViewById(R.id.tvCategoryIcon);
        TextView tvName = item.findViewById(R.id.tvCategoryName);
        TextView tvCount = item.findViewById(R.id.tvTransactionCount);
        TextView tvAmount = item.findViewById(R.id.tvCategoryAmount);
        ProgressBar progressBar = item.findViewById(R.id.pbIndicator);

        tvIcon.setText(icon);
        tvName.setText(name);
        tvCount.setText(count);
        tvAmount.setText(amount);
        progressBar.setProgress(progress);

        container.addView(item);
    }
}