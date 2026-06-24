package com.example.smartreceiptmanager;

import android.app.Application;
import com.example.smartreceiptmanager.utils.ThemeHelper;

public class SmartReceiptApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyTheme(this);
    }
}