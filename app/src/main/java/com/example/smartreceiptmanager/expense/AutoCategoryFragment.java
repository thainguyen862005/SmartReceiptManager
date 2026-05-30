package com.example.smartreceiptmanager.expense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.utils.CurrencyUtils;

public class AutoCategoryFragment extends Fragment {

    private static final String ARG_MERCHANT = "merchant";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_DATE = "date";
    private static final String ARG_RECEIPT_TEXT = "receipt_text";

    private String merchant;
    private double amount;
    private long date;
    private String receiptText;

    public static AutoCategoryFragment newInstance(
            String merchant,
            double amount,
            long date,
            String receiptText
    ) {
        AutoCategoryFragment fragment = new AutoCategoryFragment();

        Bundle args = new Bundle();
        args.putString(ARG_MERCHANT, merchant);
        args.putDouble(ARG_AMOUNT, amount);
        args.putLong(ARG_DATE, date);
        args.putString(ARG_RECEIPT_TEXT, receiptText);

        fragment.setArguments(args);
        return fragment;
    }

    public AutoCategoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_auto_category, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        loadArguments();

        TextView txtMerchantName = view.findViewById(R.id.txtMerchantName);
        TextView txtAmount = view.findViewById(R.id.txtAmount);

        txtMerchantName.setText(merchant);
        txtAmount.setText(CurrencyUtils.formatVnd(amount));

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });

        view.findViewById(R.id.cardSuggestionFood).setOnClickListener(v -> {
            openAddExpense("Ăn uống");
        });

        view.findViewById(R.id.cardSuggestionCoffee).setOnClickListener(v -> {
            openAddExpense("Ăn uống");
        });

        view.findViewById(R.id.cardSuggestionSocial).setOnClickListener(v -> {
            openAddExpense("Mua sắm");
        });

        view.findViewById(R.id.btnChooseAllCategory).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Bạn có thể chọn lại danh mục ở màn thêm chi tiêu", Toast.LENGTH_SHORT).show();
            openAddExpense("Ăn uống");
        });
    }

    private void loadArguments() {
        Bundle args = getArguments();

        if (args == null) {
            merchant = "Không xác định";
            amount = 0;
            date = System.currentTimeMillis();
            receiptText = "";
            return;
        }

        merchant = args.getString(ARG_MERCHANT, "Không xác định");
        amount = args.getDouble(ARG_AMOUNT, 0);
        date = args.getLong(ARG_DATE, System.currentTimeMillis());
        receiptText = args.getString(ARG_RECEIPT_TEXT, "");
    }

    private void openAddExpense(String category) {
        AddExpenseFragment fragment = AddExpenseFragment.newFromOcr(
                merchant,
                amount,
                date,
                receiptText,
                category
        );

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}