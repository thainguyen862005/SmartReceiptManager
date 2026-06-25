package com.example.smartreceiptmanager.expense;

import static android.os.Build.VERSION_CODES_FULL.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartreceiptmanager.R;
import com.example.smartreceiptmanager.utils.CurrencyUtils;
import com.example.smartreceiptmanager.utils.DateUtils;
import android.content.Intent;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.smartreceiptmanager.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ExpenseListFragment extends Fragment {
    private ExpenseStore expenseStore;
    private TextView txtEmpty;
    private LinearLayout layoutAllExpenses;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expenseStore = new ExpenseStore(requireContext());
        txtEmpty = view.findViewById(R.id.txtEmptyExpenseList);
        layoutAllExpenses = view.findViewById(R.id.layoutAllExpenses);

        // Đã xóa dòng setOnClickListener của nút btnAddExpenseHistory vì nút đã bị xóa bên XML

        View cardHeaderAvatar = view.findViewById(R.id.cardHeaderAvatar);
        ImageView imgHeaderAvatar = view.findViewById(R.id.imgHeaderAvatar);
        if (cardHeaderAvatar != null) {
            cardHeaderAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                startActivity(intent);
            });
        }
        if (imgHeaderAvatar != null) {
            FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null && firebaseUser.getPhotoUrl() != null) {
                    Glide.with(this)
                            .load(firebaseUser.getPhotoUrl())
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .circleCrop()
                            .into(imgHeaderAvatar);
                }
            });
        }

        renderExpenses();
    }

    private void renderExpenses() {
        List<Expense> savedExpenses = expenseStore.getAllExpenses();
        List<Expense> expenses = savedExpenses.isEmpty() ? getPreviewExpenses() : savedExpenses;
        txtEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
        layoutAllExpenses.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String currentGroup = "";
        for (int index = 0; index < expenses.size(); index++) {
            Expense expense = expenses.get(index);
            String group = getGroupTitle(expense, index);
            if (!group.equals(currentGroup)) {
                currentGroup = group;
                layoutAllExpenses.addView(createGroupHeader(group));
            }

            View itemView = inflater.inflate(R.layout.item_expense, layoutAllExpenses, false);
            TextView txtIcon = itemView.findViewById(R.id.txtExpenseIcon);
            TextView txtMerchant = itemView.findViewById(R.id.txtMerchant);
            TextView txtMeta = itemView.findViewById(R.id.txtMeta);
            TextView txtAmount = itemView.findViewById(R.id.txtAmount);
            TextView btnDelete = itemView.findViewById(R.id.btnDeleteExpense);

            txtIcon.setText(getCategoryIcon(expense.getCategory()));
            txtIcon.setBackgroundResource(getCategoryBackground(expense.getCategory()));
            txtMerchant.setText(expense.getMerchantName());
            txtMeta.setText(getTimeText(expense, index) + " • " + expense.getCategory());

            if ("Thu nhập".equals(expense.getCategory())) {
                txtAmount.setText("+ " + CurrencyUtils.formatVnd(expense.getAmount()));
                txtAmount.setTextColor(getResources().getColor(R.color.primary_green));
            } else {
                txtAmount.setText("- " + CurrencyUtils.formatVnd(expense.getAmount()));
                txtAmount.setTextColor(0xFFB9181E);
            }

            itemView.setOnClickListener(v -> openExpenseDetail(expense.getId()));
            btnDelete.setOnClickListener(v -> openExpenseDetail(expense.getId()));

            layoutAllExpenses.addView(itemView);
        }
    }

    private TextView createGroupHeader(String title) {
        TextView header = new TextView(requireContext());
        header.setText(title);
        header.setTextColor(0xFF435047);
        header.setTextSize(22);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 18, 0, 14);
        return header;
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

    private List<Expense> getPreviewExpenses() {
        long now = System.currentTimeMillis();
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("preview-h1", "Ăn trưa Highland", 150000, "Ăn uống", now, "", "", false, now, now));
        expenses.add(new Expense("preview-h2", "Grab bi làm", 45000, "Di chuyển", now, "", "", false, now, now));
        expenses.add(new Expense("preview-h3", "Lương tháng 9", 15000000, "Thu nhập", now - 86400000, "", "", false, now, now));
        expenses.add(new Expense("preview-h4", "Siêu thị Winmart", 320000, "Sinh hoạt", now - 86400000, "", "", false, now, now));
        return expenses;
    }

    private String getGroupTitle(Expense expense, int index) {
        return DateUtils.formatDate(expense.getDate());
    }

    private String getTimeText(Expense expense, int index) {
        if (index == 0) return "12:30";
        if (index == 1) return "08:15";
        if (index == 2) return "15:00";
        if (index == 3) return "19:45";
        return DateUtils.formatDate(expense.getDate());
    }

    private String getCategoryIcon(String category) {
        if ("Ăn uống".equals(category)) {
            return "🍴";
        } else if ("Di chuyển".equals(category)) {
            return "🚗";
        } else if ("Mua sắm".equals(category)) {
            return "🛍";
        } else if ("Hóa đơn".equals(category)) {
            return "🧾";
        } else if ("Thu nhập".equals(category)) {
            return "💵";
        } else if ("Sinh hoạt".equals(category)) {
            return "🛍";
        }

        return "💸";
    }

    private int getCategoryBackground(String category) {
        if ("Ăn uống".equals(category) || "Thu nhập".equals(category)) {
            return R.drawable.bg_avatar_green;
        } else if ("Di chuyển".equals(category)) {
            return R.drawable.bg_avatar_blue;
        } else if ("Sinh hoạt".equals(category)) {
            return R.drawable.bg_avatar_mint;
        }

        return R.drawable.bg_avatar_gray;
    }
}