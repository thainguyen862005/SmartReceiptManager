package com.example.smartreceiptmanager.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper.showNotification(
                context,
                "Nhắc nhở quét hóa đơn",
                "Đã đến lúc ghi chép chi tiêu hôm nay rồi! Hãy mở app để quét hóa đơn nhé."
        );
    }
}
