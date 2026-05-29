package com.example.smartreceiptmanager.expense;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;

public class AddExpenseFragment extends Fragment {

    private EditText edtAmount;
    private View cardFood, cardTransport, cardShopping, cardBill;

    private String selectedCategory = "Ăn uống";

    public AddExpenseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_add_expense, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        edtAmount = view.findViewById(R.id.edtAmount);

        cardFood = view.findViewById(R.id.cardFood);
        cardTransport = view.findViewById(R.id.cardTransport);
        cardShopping = view.findViewById(R.id.cardShopping);
        cardBill = view.findViewById(R.id.cardBill);

        View btnClose = view.findViewById(R.id.btnClose);
        View btnSave = view.findViewById(R.id.btnSaveExpense);

        btnClose.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();

            View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
        });

        cardFood.setOnClickListener(v -> selectCategory("Ăn uống"));
        cardTransport.setOnClickListener(v -> selectCategory("Di chuyển"));
        cardShopping.setOnClickListener(v -> selectCategory("Mua sắm"));
        cardBill.setOnClickListener(v -> selectCategory("Hóa đơn"));

        btnSave.setOnClickListener(v -> saveExpense());

        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        selectCategory("Ăn uống");
    }

    private void selectCategory(String category) {
        selectedCategory = category;

        cardFood.setSelected(false);
        cardTransport.setSelected(false);
        cardShopping.setSelected(false);
        cardBill.setSelected(false);

        if (category.equals("Ăn uống")) {
            cardFood.setSelected(true);
        } else if (category.equals("Di chuyển")) {
            cardTransport.setSelected(true);
        } else if (category.equals("Mua sắm")) {
            cardShopping.setSelected(true);
        } else if (category.equals("Hóa đơn")) {
            cardBill.setSelected(true);
        }
    }

    private void saveExpense() {
        String amountText = edtAmount.getText().toString().trim();

        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(
                requireContext(),
                "Đã lưu: " + amountText + "đ - " + selectedCategory,
                Toast.LENGTH_SHORT
        ).show();

        requireActivity()
                .getSupportFragmentManager()
                .popBackStack();

        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }
}