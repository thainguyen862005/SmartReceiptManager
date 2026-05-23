package com.example.smartreceiptmanager;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ExpenseAdapter extends BaseAdapter {
    private final Activity activity;
    private final ArrayList<Expense> expenses;
    private final NumberFormat currencyFormat;

    public ExpenseAdapter(Activity activity, ArrayList<Expense> expenses) {
        this.activity = activity;
        this.expenses = expenses;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    @Override
    public int getCount() { return expenses.size(); }

    @Override
    public Object getItem(int position) { return expenses.get(position); }

    @Override
    public long getItemId(int position) { return expenses.get(position).getId(); }

    static class ViewHolder {
        TextView txtCategoryIcon, txtTitle, txtAmount, txtInfo, txtNote;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.item_expense, parent, false);
            holder = new ViewHolder();
            holder.txtCategoryIcon = convertView.findViewById(R.id.txtCategoryIcon);
            holder.txtTitle = convertView.findViewById(R.id.txtTitle);
            holder.txtAmount = convertView.findViewById(R.id.txtAmount);
            holder.txtInfo = convertView.findViewById(R.id.txtInfo);
            holder.txtNote = convertView.findViewById(R.id.txtNote);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Expense expense = expenses.get(position);
        holder.txtCategoryIcon.setText(getIcon(expense.getCategory()));
        holder.txtTitle.setText(expense.getTitle());
        holder.txtAmount.setText(currencyFormat.format(expense.getAmount()));
        holder.txtInfo.setText(expense.getCategory() + " • " + expense.getDate());
        holder.txtNote.setText(expense.getNote().isEmpty() ? "Không có ghi chú" : expense.getNote());
        return convertView;
    }

    private String getIcon(String category) {
        if (category == null) return "💳";
        String c = category.toLowerCase(Locale.ROOT);
        if (c.contains("ăn") || c.contains("uong") || c.contains("uống")) return "🍜";
        if (c.contains("học") || c.contains("hoc")) return "📚";
        if (c.contains("di") || c.contains("chuyển") || c.contains("xăng")) return "🛵";
        if (c.contains("mua")) return "🛒";
        if (c.contains("giải") || c.contains("giai")) return "🎮";
        return "💳";
    }
}
