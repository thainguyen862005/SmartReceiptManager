package com.example.smartreceiptmanager.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.smartreceiptmanager.ProfileActivity;
import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.auth.AuthViewModel;
import com.example.smartreceiptmanager.expense.Expense;
import com.example.smartreceiptmanager.expense.ExpenseDetailFragment;
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.example.smartreceiptmanager.utils.DateUtils;

// IMPORT CÁC THƯ VIỆN FIREBASE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private TextView txtBalance;
    private TextView txtWeekTotal;
    private TextView txtEmptyExpense;
    private LinearLayout layoutExpenseList;
    private AuthViewModel authViewModel;
    private ImageView imgHeaderAvatar;
    private View cardHeaderAvatar;

    // Các biến lắng nghe Firebase để giải phóng bộ nhớ khi đóng Fragment
    private DatabaseReference transactionsRef;
    private DatabaseReference walletRef;
    private ValueEventListener transactionsListener;
    private ValueEventListener walletListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardHeaderAvatar = view.findViewById(R.id.cardHeaderAvatar);
        imgHeaderAvatar = view.findViewById(R.id.imgHeaderAvatar);
        txtBalance = view.findViewById(R.id.txtBalance);
        txtWeekTotal = view.findViewById(R.id.txtWeekTotal);
        txtEmptyExpense = view.findViewById(R.id.txtEmptyExpense);
        layoutExpenseList = view.findViewById(R.id.layoutExpenseList);

        // 1. Quản lý thông tin Avatar người dùng
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        authViewModel.getUserProfileLiveData().observe(getViewLifecycleOwner(), userProfile -> {
            if (userProfile != null && userProfile.getProfile() != null) {
                String avatarUrl = userProfile.getProfile().getAvatar_url();
                if (avatarUrl != null && !avatarUrl.isEmpty() && imgHeaderAvatar != null) {
                    if (avatarUrl.startsWith("http")) {
                        Glide.with(this).load(avatarUrl).placeholder(android.R.drawable.sym_def_app_icon).circleCrop().into(imgHeaderAvatar);
                    } else {
                        try {
                            byte[] bytes = android.util.Base64.decode(avatarUrl, android.util.Base64.DEFAULT);
                            Glide.with(this).load(bytes).placeholder(android.R.drawable.sym_def_app_icon).circleCrop().into(imgHeaderAvatar);
                        } catch (Exception ignored) {}
                    }
                }
            }
        });

        if (cardHeaderAvatar != null) {
            cardHeaderAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                startActivity(intent);
            });
        }

        // 2. Chuyển Tab Lịch sử khi nhấn "Xem tất cả"
        view.findViewById(R.id.btnViewAll).setOnClickListener(v -> {
            requireActivity().findViewById(R.id.btnHistory).performClick();
        });

        // 3. Khởi chạy tiến trình kết nối & đọc dữ liệu từ Firebase
        setupFirebaseRealtime();
    }

    private void setupFirebaseRealtime() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            txtEmptyExpense.setVisibility(View.VISIBLE);
            txtEmptyExpense.setText("Vui lòng đăng nhập lại");
            return;
        }

        String uid = currentUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // --- ĐOẠN ĐỌC SỐ DƯ VÍ TỪ FIREBASE ---
        walletRef = database.getReference("User_Profiles").child("wallets").child(uid).child("wallet_default_01");
        walletListener = walletRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long totalAmount = snapshot.child("total_amount").getValue(Long.class);
                    if (totalAmount != null) {
                        txtBalance.setText(CurrencyUtils.formatVnd(totalAmount));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // --- ĐOẠN ĐỌC DANH SÁCH GIAO DỊCH TỪ FIREBASE ---
        transactionsRef = database.getReference("User_Profiles").child("transactions").child(uid);
        transactionsListener = transactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Expense> firebaseExpenses = new ArrayList<>();
                long totalExpenseThisMonth = 0;

                // Vòng lặp quét qua từng transaction_id
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String type = snapshot.child("type").getValue(String.class);

                    // Chỉ lấy những giao dịch là khoản chi (expense)
                    if ("expense".equals(type)) {
                        String id = snapshot.getKey();
                        Long amount = snapshot.child("amount").getValue(Long.class);
                        String note = snapshot.child("note").getValue(String.class);
                        String categoryName = snapshot.child("category").child("name").getValue(String.class);
                        Long createdAt = snapshot.child("created_at").getValue(Long.class);

                        long finalAmount = (amount != null) ? amount : 0;
                        long finalTime = (createdAt != null) ? createdAt : System.currentTimeMillis();
                        String merchant = (note != null && !note.isEmpty()) ? note : "Chi tiêu không tên";

                        // Ánh xạ về Object Expense (Phù hợp với cấu trúc hàm tạo của bạn)
                        Expense expense = new Expense(id, merchant, finalAmount, categoryName, finalTime, note, "", false, finalTime, finalTime);

                        // Đẩy các giao dịch mới nhất lên đầu danh sách hiển thị
                        firebaseExpenses.add(0, expense);

                        // Tính tổng tiền chi tiêu tạm thời
                        totalExpenseThisMonth += finalAmount;
                    }
                }
                renderExpenses(firebaseExpenses, totalExpenseThisMonth, uid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(requireContext(), "Lỗi tải Firebase: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderExpenses(List<Expense> expenses, long totalExpense, String uid) {
        layoutExpenseList.removeAllViews();
        txtWeekTotal.setText(CurrencyUtils.formatVnd(totalExpense));

        if (expenses.isEmpty()) {
            txtEmptyExpense.setVisibility(View.VISIBLE);
            return;
        }

        txtEmptyExpense.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        int count = Math.min(expenses.size(), 3);
        for (int i = 0; i < count; i++) {
            Expense expense = expenses.get(i);
            View itemView = inflater.inflate(R.layout.item_expense, layoutExpenseList, false);

            TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
            TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
            TextView txtMeta = itemView.findViewById(R.id.txtMeta);
            TextView txtAmount = itemView.findViewById(R.id.txtAmount);
            TextView btnDelete = itemView.findViewById(R.id.btnDeleteExpense);

            txtIcon.setText(getCategoryIcon(expense.getCategory()));
            txtIcon.setBackgroundResource(getCategoryBackground(expense.getCategory()));
            txtMerchant.setText(expense.getMerchantName());
            txtMeta.setText(getFriendlyDate(expense));
            txtAmount.setText("-" + CurrencyUtils.formatVnd(expense.getAmount()));

            itemView.setOnClickListener(v -> openExpenseDetail(expense.getId()));
            btnDelete.setOnClickListener(v -> {
                if (transactionsRef != null) {
                    transactionsRef.child(expense.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Đã xóa khoản chi", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });

            layoutExpenseList.addView(itemView);
        }
    }

    private void openExpenseDetail(String expenseId) {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, ExpenseDetailFragment.newInstance(expenseId))
                .addToBackStack(null)
                .commit();

        View bottomNav = requireActivity().findViewById(R.id.custom_bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
    }

    private String getFriendlyDate(Expense expense) {
        if (expense.getNote() != null && expense.getNote().startsWith("Hôm")) {
            return expense.getNote();
        }
        return DateUtils.formatDate(expense.getDate());
    }

    private String getCategoryIcon(String category) {
        if ("Ăn uống".equals(category)) return "🍴";
        if ("Di chuyển".equals(category)) return "🚗";
        if ("Mua sắm".equals(category)) return "🛍";
        if ("Hóa đơn".equals(category)) return "🧾";
        return "💸";
    }

    private int getCategoryBackground(String category) {
        if ("Ăn uống".equals(category)) return R.drawable.bg_avatar_mint;
        if ("Di chuyển".equals(category)) return R.drawable.bg_avatar_blue;
        return R.drawable.bg_avatar_gray;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionsRef != null && transactionsListener != null) {
            transactionsRef.removeEventListener(transactionsListener);
        }
        if (walletRef != null && walletListener != null) {
            walletRef.removeEventListener(walletListener);
        }
    }
}