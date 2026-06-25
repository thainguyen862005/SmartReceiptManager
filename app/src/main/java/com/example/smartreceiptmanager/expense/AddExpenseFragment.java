package com.example.smartreceiptmanager.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.firestore.SyncManager;
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.example.smartreceiptmanager.utils.DateUtils;

import java.text.DecimalFormat;
import java.util.Calendar;

public class AddExpenseFragment extends Fragment {
    private static final String ARG_EXPENSE_ID = "expense_id";
    private static final String ARG_OCR_AMOUNT = "ocr_amount";
    private static final String ARG_OCR_MERCHANT = "ocr_merchant";
    private static final String ARG_OCR_DATE = "ocr_date";
    private static final String ARG_OCR_TEXT = "ocr_text";
    private static final String ARG_CATEGORY = "category";

    private EditText edtAmount;
    private EditText edtMerchant;
    private EditText edtNote;
    private EditText edtReceiptText;
    private TextView txtScreenTitle;
    private TextView txtDate;
    private TextView btnSave;
    private TextView btnDelete;
    private View cardFood, cardTransport, cardShopping, cardBill;

    private ExpenseStore expenseStore;
    private Expense editingExpense;
    private String selectedCategory = "Ăn uống";
    private long selectedDate = System.currentTimeMillis();

    public static AddExpenseFragment newEditInstance(String expenseId) {
        AddExpenseFragment fragment = new AddExpenseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXPENSE_ID, expenseId);
        fragment.setArguments(args);
        return fragment;
    }

    public static AddExpenseFragment newFromOcr(
            String merchant,
            double amount,
            long date,
            String receiptText,
            String category
    ) {
        AddExpenseFragment fragment = new AddExpenseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OCR_MERCHANT, merchant);
        args.putDouble(ARG_OCR_AMOUNT, amount);
        args.putLong(ARG_OCR_DATE, date);
        args.putString(ARG_OCR_TEXT, receiptText);
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    public AddExpenseFragment() {

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

        expenseStore = new ExpenseStore(requireContext());
        edtAmount = view.findViewById(R.id.edtAmount);
        edtMerchant = view.findViewById(R.id.edtMerchant);
        edtNote = view.findViewById(R.id.edtNote);
        edtReceiptText = view.findViewById(R.id.edtReceiptText);
        txtScreenTitle = view.findViewById(R.id.txtScreenTitle);
        txtDate = view.findViewById(R.id.txtDate);

        cardFood = view.findViewById(R.id.cardFood);
        cardTransport = view.findViewById(R.id.cardTransport);
        cardShopping = view.findViewById(R.id.cardShopping);
        cardBill = view.findViewById(R.id.cardBill);

        View btnClose = view.findViewById(R.id.btnClose);
        btnSave = view.findViewById(R.id.btnSaveExpense);
        btnDelete = view.findViewById(R.id.btnDeleteExpense);

        btnClose.setOnClickListener(v -> {
            closeScreen();
        });

        cardFood.setOnClickListener(v -> selectCategory("Ăn uống"));
        cardTransport.setOnClickListener(v -> selectCategory("Di chuyển"));
        cardShopping.setOnClickListener(v -> selectCategory("Mua sắm"));
        cardBill.setOnClickListener(v -> selectCategory("Hóa đơn"));

        btnSave.setOnClickListener(v -> saveExpense());
        btnDelete.setOnClickListener(v -> deleteExpense());
        view.findViewById(R.id.layoutDate).setOnClickListener(v -> showDatePicker());

        edtAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    edtAmount.removeTextChangedListener(this);
                    String cleanString = s.toString().replace(".", "");
                    if (!cleanString.isEmpty()) {
                        long value = Long.parseLong(cleanString);
                        DecimalFormat formatter = new DecimalFormat("#,###");
                        current = formatter.format(value).replace(",", ".");
                        edtAmount.setText(current);
                        edtAmount.setSelection(current.length());
                    }
                    edtAmount.addTextChangedListener(this);
                }
            }
        });

        loadInitialData();
    }

    private void selectCategory(String category) {
        selectedCategory = category;

        cardFood.setSelected(false);
        cardTransport.setSelected(false);
        cardShopping.setSelected(false);
        cardBill.setSelected(false);
        cardFood.setBackgroundResource(R.drawable.bg_category_normal_figma);
        cardTransport.setBackgroundResource(R.drawable.bg_category_normal_figma);
        cardShopping.setBackgroundResource(R.drawable.bg_category_normal_figma);
        cardBill.setBackgroundResource(R.drawable.bg_category_normal_figma);

        if (category.equals("Ăn uống")) {
            cardFood.setSelected(true);
            cardFood.setBackgroundResource(R.drawable.bg_category_selected_figma);
        } else if (category.equals("Di chuyển")) {
            cardTransport.setSelected(true);
            cardTransport.setBackgroundResource(R.drawable.bg_category_selected_figma);
        } else if (category.equals("Mua sắm")) {
            cardShopping.setSelected(true);
            cardShopping.setBackgroundResource(R.drawable.bg_category_selected_figma);
        } else if (category.equals("Hóa đơn")) {
            cardBill.setSelected(true);
            cardBill.setBackgroundResource(R.drawable.bg_category_selected_figma);
        }
    }

    private void loadInitialData() {
        Bundle args = getArguments();

        if (args != null && args.containsKey(ARG_EXPENSE_ID)) {
            editingExpense = expenseStore.getExpenseById(args.getString(ARG_EXPENSE_ID));
        }

        if (editingExpense != null) {
            txtScreenTitle.setText("Sửa chi tiêu");
            btnSave.setText("Cập nhật ✓");
            btnDelete.setVisibility(View.VISIBLE);

            edtAmount.setText(CurrencyUtils.formatAmount(editingExpense.getAmount()));
            edtMerchant.setText(editingExpense.getMerchantName());
            edtNote.setText(editingExpense.getNote());
            edtReceiptText.setText(editingExpense.getReceiptText());
            selectedDate = editingExpense.getDate();
            selectCategory(editingExpense.getCategory());
        } else {
            txtScreenTitle.setText("Thêm chi tiêu");
            btnSave.setText("Lưu chi tiêu ✓");
            btnDelete.setVisibility(View.GONE);
            selectCategory("Ăn uống");

            if (args != null) {
                String merchant = args.getString(ARG_OCR_MERCHANT, "");
                double amount = args.getDouble(ARG_OCR_AMOUNT, 0);
                long date = args.getLong(ARG_OCR_DATE, 0);
                String receiptText = args.getString(ARG_OCR_TEXT, "");
                String category = args.getString(ARG_CATEGORY, "Ăn uống");

                edtMerchant.setText(merchant);

                if (amount > 0) {
                    edtAmount.setText(String.valueOf((long) amount));
                }

                if (date > 0) {
                    selectedDate = date;
                }

                edtReceiptText.setText(receiptText);
                edtReceiptText.setVisibility(receiptText.trim().isEmpty() ? View.GONE : View.VISIBLE);

                selectCategory(category);
            }
        }

        updateDateText();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    selectedDate = selected.getTimeInMillis();
                    updateDateText();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateText() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);
        txtDate.setText("Hôm nay, " + calendar.get(Calendar.DAY_OF_MONTH)
                + " Th" + (calendar.get(Calendar.MONTH) + 1)
                + " " + calendar.get(Calendar.YEAR));
    }

    private void saveExpense() {
        String amountText = edtAmount.getText().toString().trim();
        String merchant = edtMerchant.getText().toString().trim();
        String note = edtNote.getText().toString().trim();
        String receiptText = edtReceiptText.getText().toString().trim();

        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        if (merchant.trim().isEmpty()) {
            edtMerchant.setError("Vui lòng nhập nơi chi tiêu");
            edtMerchant.requestFocus();
            return;
        }
        if (selectedCategory == null ||
                selectedCategory.trim().isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = CurrencyUtils.parseAmount(amountText);
        if (amount <= 0) {
            if (editingExpense == null && expenseStore.isDuplicate(merchant, amount, selectedDate)) {
                Toast.makeText(requireContext(), "Giao dịch đã tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Expense expense = editingExpense == null ? new Expense() : editingExpense;
        expense.setAmount(amount);
        expense.setMerchantName(merchant);
        expense.setCategory(selectedCategory);
        expense.setDate(selectedDate);
        expense.setNote(note);
        expense.setReceiptText(receiptText);

        expenseStore.saveExpense(expense);

        // Trigger sync lên Firestore ngay sau khi lưu local
        SyncManager.getInstance(requireContext()).syncSingleExpense(expense);

        String message = editingExpense == null ? "Đã lưu khoản chi" : "Đã cập nhật khoản chi";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        closeScreen();
    }

    private void deleteExpense() {
        if (editingExpense == null) {
            return;
        }

        expenseStore.deleteExpense(editingExpense.getId());
        Toast.makeText(requireContext(), "Đã xóa khoản chi", Toast.LENGTH_SHORT).show();
        closeScreen();
    }

    private void closeScreen() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new com.example.smartreceiptmanager.home.HomeFragment())
                .commit();

        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }
}
