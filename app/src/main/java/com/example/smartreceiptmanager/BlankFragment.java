package com.example.smartreceiptmanager;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.test.CategoryExpense;
import com.example.smartreceiptmanager.test.MockDatabase;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BlankFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout của Fragment chính
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        // 1. Tìm đúng LinearLayout container trong file XML của bạn
        LinearLayout containerLayout = view.findViewById(R.id.layoutTopCategoriesContainer);

        // 2. Lấy List data từ class MockDatabase vừa tạo
        List<CategoryExpense> mockData = MockDatabase.getTopCategories();

        // Định dạng format tiền tệ Việt Nam (đ) cho chuẩn chỉnh
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Xóa các phần tử cũ phòng hờ trùng lặp
        if (containerLayout != null) {
            containerLayout.removeAllViews();

            // 3. Duyệt danh sách bằng vòng lặp for-each
            for (CategoryExpense category : mockData) {

                // Thu phóng (Inflate) layout hàng đơn lẻ
                View itemView = inflater.inflate(R.layout.item_top_category, containerLayout, false);

                // Ánh xạ các view con bên trong hàng đó
                TextView tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
                CardView cardIconBg = itemView.findViewById(R.id.cardIconBg);
                TextView tvName = itemView.findViewById(R.id.tvCategoryName);
                TextView tvCount = itemView.findViewById(R.id.tvTransactionCount);
                TextView tvAmount = itemView.findViewById(R.id.tvCategoryAmount);
                ProgressBar pbIndicator = itemView.findViewById(R.id.pbIndicator);

                // Set data tương ứng vào view
                tvIcon.setText(category.getIcon());
                tvName.setText(category.getName());
                tvCount.setText(category.getTransactionCount() + " giao dịch");
                tvAmount.setText(currencyFormatter.format(category.getAmount()));

                // Set phần trăm thanh tiến độ ProgressBar
                pbIndicator.setProgress(category.getPercentage());

                // Xử lý màu sắc động từ mã Hex Code
                int colorInt = Color.parseColor(category.getColorHex());
                pbIndicator.setProgressTintList(ColorStateList.valueOf(colorInt));

                // Tạo hiệu ứng nền bo tròn mờ cho Icon (độ trong suốt alpha ~ 40)
                if (cardIconBg != null) {
                    cardIconBg.setCardBackgroundColor(colorInt);
                    cardIconBg.setBackgroundTintList(ColorStateList.valueOf(colorInt).withAlpha(40));
                }

                // Cuối cùng, add view dòng này vào container lớn
                containerLayout.addView(itemView);
            }
        }

        return view;
    }
}