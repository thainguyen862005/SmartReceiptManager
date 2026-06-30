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

// THÊM CÁC IMPORT THƯ VIỆN FIREBASE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

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

        loadInitialData();
        setupKeyboardAwareScroll(view);
    }
    private int lastImeHeight = 0;

    /*
     * Hàm xử lý để ô nhập (vd: Ghi chú, Nơi chi tiêu) không bị bàn phím
     * ảo che mất khi người dùng đang gõ.
     *
     * Cơ chế hoạt động:
     * 1. Lắng nghe sự kiện WindowInsets mỗi khi bàn phím mở/đóng.
     * 2. Tính đúng paddingBottom của ScrollView theo từng trạng thái:
     *    - Bàn phím đang mở: chỉ cần đệm nhỏ (20dp) + chiều cao bàn phím,
     *      KHÔNG cộng thêm khoảng đệm 120dp gốc (vì thanh nút "Lưu chi tiêu"
     *      lúc này đang bị bàn phím che, không cần chừa chỗ cho nó nữa).
     *    - Bàn phím đóng: dùng lại khoảng đệm gốc 120dp để chừa chỗ
     *      cho thanh nút cố định ở đáy màn hình.
     * 3. Khi bàn phím vừa mở lên lần đầu (justOpened = true), tự động
     *    cuộn ScrollView tới đúng vị trí ô đang được focus, để người
     *    dùng luôn nhìn thấy chữ mình đang gõ.
     */
    private void setupKeyboardAwareScroll(View rootView) {
        android.widget.ScrollView scrollView = rootView.findViewById(R.id.scrollView);
         /* Khoảng đệm gốc dành cho thanh nút "Lưu chi tiêu" cố định ở đáy,chỉ áp dụng khi bàn phím đang đóng. */
        int basePaddingBottom = dpToPx(120);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int imeHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;

    /* Lấy chiều cao thực tế của bàn phím ảo (đơn vị px).bằng 0 nếu bàn phím đang đóng. */
            int paddingBottom = imeHeight > 0 ? imeHeight + dpToPx(20) : basePaddingBottom;

            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    scrollView.getPaddingTop(),
                    scrollView.getPaddingRight(),
                    paddingBottom
            );

            boolean justOpened = imeHeight > 0 && lastImeHeight == 0;
            lastImeHeight = imeHeight;

            if (justOpened) {
                View focused = rootView.findFocus();
                if (focused != null) {
                    scrollView.post(() -> {
                        /* Lấy vị trí tuyệt đối của ô đang focus trên màn hình */
                        int[] loc = new int[2];
                        focused.getLocationOnScreen(loc);
                         /* Lấy vị trí tuyệt đối của ScrollView trên màn hình
                       để tính ra vị trí tương đối của ô nhập bên trong nó */
                        int[] scrollLoc = new int[2];
                        scrollView.getLocationOnScreen(scrollLoc);
                        /* Vị trí Y mục tiêu cần cuộn tới = vị trí ô nhập
                        (tính từ đỉnh ScrollView) cộng với scroll hiện tại */
                        int targetY = loc[1] - scrollLoc[1] + scrollView.getScrollY();
                        scrollView.smoothScrollTo(0, targetY - 8);
                    });
                }
            }

            return insets;
        });

        rootView.requestApplyInsets();
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
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

            edtAmount.setText(String.valueOf((long) editingExpense.getAmount()));
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

        double amount = CurrencyUtils.parseAmount(amountText);
        if (amount <= 0) {
            Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (merchant.isEmpty()) {
            merchant = note.isEmpty() ? selectedCategory : note;
        }

        Expense expense = editingExpense == null ? new Expense() : editingExpense;
        expense.setAmount(amount);
        expense.setMerchantName(merchant);
        expense.setCategory(selectedCategory);
        expense.setDate(selectedDate);
        expense.setNote(note);
        expense.setReceiptText(receiptText);

        // 1. Lưu vào SQLite cục bộ
        expenseStore.saveExpense(expense);

        // 2. TỰ ĐỘNG ĐẨY LÊN FIREBASE REALTIME DATABASE (THEO TỪNG USER RIÊNG BIỆT)
        saveExpenseToRealtimeDatabase(expense);

        // 3. Trigger sync lên Firestore ngay sau khi lưu local
        SyncManager.getInstance(requireContext()).syncSingleExpense(expense);

        String message = editingExpense == null ? "Đã lưu khoản chi" : "Đã cập nhật khoản chi";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        closeScreen();
    }

    private void deleteExpense() {
        if (editingExpense == null) {
            return;
        }

        String idToDelete = editingExpense.getId();

        // 1. Xóa ở cục bộ SQLite
        expenseStore.deleteExpense(idToDelete);

        // 2. XÓA TRÊN FIREBASE REALTIME DATABASE CỦA USER ĐÓ
        deleteExpenseFromRealtimeDatabase(idToDelete);

        Toast.makeText(requireContext(), "Đã xóa khoản chi", Toast.LENGTH_SHORT).show();
        closeScreen();
    }

    /**
     * Hàm lấy mã login hiện tại và đẩy dữ liệu lên Firebase
     */
    private void saveExpenseToRealtimeDatabase(Expense expense) {
        // 1. Check tài khoản hiện tại trên Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // 2. Lấy mã login (UID) của user đang dùng app
            String uid = currentUser.getUid();

            // 3. Trỏ thẳng vào nhánh: User_Profiles -> transactions -> [Mã login hiện tại]
            DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                    .getReference("User_Profiles")
                    .child("transactions")
                    .child(uid);

            // 4. Lấy ID giao dịch cũ (nếu đang sửa) hoặc tự tạo ID mới (nếu thêm mới)
            String transactionId = expense.getId();
            if (transactionId == null || transactionId.isEmpty()) {
                transactionId = databaseReference.push().getKey();
                if (transactionId != null) {
                    expense.setId(transactionId);
                }
            }

            if (transactionId == null) return; // Nếu lỗi không tạo được mã thì dừng

            // Định dạng thời gian
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String dateStr = sdf.format(new Date(expense.getDate()));

            // Gom dữ liệu để đẩy lên
            HashMap<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("amount", expense.getAmount());
            transactionMap.put("created_at", System.currentTimeMillis());
            transactionMap.put("note", expense.getNote() != null ? expense.getNote() : "");
            transactionMap.put("payment_method", "Tiền mặt");
            transactionMap.put("receipt_image_url", "");
            transactionMap.put("transaction_date", dateStr);
            transactionMap.put("type", "expense");
            transactionMap.put("wallet_id", "wallet_default_01");

            // Danh mục con
            HashMap<String, String> categoryMap = new HashMap<>();
            categoryMap.put("id", "cate_expense_" + expense.getCategory().hashCode());
            categoryMap.put("name", expense.getCategory());
            transactionMap.put("category", categoryMap);

            // 5. Lưu dữ liệu lên Firebase
            databaseReference.child(transactionId).setValue(transactionMap);

        } else {
            // Dự phòng trường hợp lỗi (app bị mất phiên đăng nhập ngầm)
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy thông tin đăng nhập!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hàm lấy mã login hiện tại và xóa giao dịch tương ứng
     */
    private void deleteExpenseFromRealtimeDatabase(String transactionId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && transactionId != null && !transactionId.isEmpty()) {
            // Lấy mã login (UID)
            String uid = currentUser.getUid();

            // Trỏ đúng vào nhánh của mã login hiện tại và xóa
            FirebaseDatabase.getInstance()
                    .getReference("User_Profiles")
                    .child("transactions")
                    .child(uid)
                    .child(transactionId)
                    .removeValue();
        }
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