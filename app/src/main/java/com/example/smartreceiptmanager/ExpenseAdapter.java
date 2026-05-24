package com.example.smartreceiptmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private Context context;
    private ArrayList<Expense> expenseList;

    public ExpenseAdapter(Context context, ArrayList<Expense> expenseList) {
        this.context = context;
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_expense, parent, false);

        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {

        Expense expense = expenseList.get(position);

        holder.txtTitle.setText(expense.getTitle());

        holder.txtAmount.setText(
                String.format("%,.0f VNĐ", expense.getAmount())
        );

        holder.txtCategory.setText(expense.getCategory());

        holder.txtDate.setText(expense.getDate());

        // =========================
        // Nút sửa
        // =========================
        holder.btnEdit.setOnClickListener(v -> {

            Intent intent = new Intent(context, AddExpenseActivity.class);

            intent.putExtra("id", expense.getId());
            intent.putExtra("title", expense.getTitle());
            intent.putExtra("amount", expense.getAmount());
            intent.putExtra("category", expense.getCategory());
            intent.putExtra("date", expense.getDate());
            intent.putExtra("note", expense.getNote());

            context.startActivity(intent);
        });

        // =========================
        // Nút xóa
        // =========================
        holder.btnDelete.setOnClickListener(v -> {

            new AlertDialog.Builder(context)
                    .setTitle("Xóa khoản chi")
                    .setMessage("Bạn có chắc muốn xóa khoản chi này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {

                        expenseList.remove(position);

                        notifyItemRemoved(position);

                        notifyItemRangeChanged(position, expenseList.size());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    // =====================================
    // ViewHolder
    // =====================================

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        TextView txtAmount;
        TextView txtCategory;
        TextView txtDate;

        ImageButton btnEdit;
        ImageButton btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtDate = itemView.findViewById(R.id.txtDate);

            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
