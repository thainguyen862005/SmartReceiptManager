package com.example.smartreceiptmanager.expense;

import java.util.HashMap;
import java.util.Map;

public class CategorySuggestionEngine {
    private final Map<String, String> keywordMap = new HashMap<>();
    public CategorySuggestionEngine() {
        loadKeywords();
    }

    private void loadKeywords() {
        keywordMap.put("highlands","Ăn uống");
        keywordMap.put("highland","Ăn uống");
        keywordMap.put("coffee","Ăn uống");
        keywordMap.put("cafe","Ăn uống");
        keywordMap.put("phở","Ăn uống");
        keywordMap.put("pho","Ăn uống");
        keywordMap.put("bún","Ăn uống");
        keywordMap.put("bun","Ăn uống");
        keywordMap.put("pizza","Ăn uống");
        keywordMap.put("kfc","Ăn uống");
        keywordMap.put("lotteria","Ăn uống");
        keywordMap.put("mcdonald","Ăn uống");
        keywordMap.put("starbucks","Ăn uống");
        keywordMap.put("mixue","Ăn uống");
        keywordMap.put("gong cha","Ăn uống");

        keywordMap.put("grab","Di chuyển");
        keywordMap.put("be","Di chuyển");
        keywordMap.put("xanh sm","Di chuyển");
        keywordMap.put("taxi","Di chuyển");
        keywordMap.put("vinbus","Di chuyển");
        keywordMap.put("xe buýt","Di chuyển");

        keywordMap.put("winmart","Mua sắm");
        keywordMap.put("coopmart","Mua sắm");
        keywordMap.put("circle k","Mua sắm");
        keywordMap.put("bách hóa","Mua sắm");
        keywordMap.put("bhx","Mua sắm");
        keywordMap.put("guardian","Mua sắm");
        keywordMap.put("pharmacity","Mua sắm");

        keywordMap.put("điện lực","Hóa đơn");
        keywordMap.put("evn","Hóa đơn");
        keywordMap.put("vnpt","Hóa đơn");
        keywordMap.put("viettel","Hóa đơn");
        keywordMap.put("mobifone","Hóa đơn");
        keywordMap.put("fpt","Hóa đơn");
        keywordMap.put("nước","Hóa đơn");
    }

    public String suggestCategory(String merchant) {
        if (merchant == null || merchant.trim().isEmpty()) {
            return "Khác";
        }

        String text = merchant.trim().toLowerCase();
        for(String keyword : keywordMap.keySet()){
            if(text.contains(keyword)){
                return keywordMap.get(keyword);
            }
        }

        return "Khác";
    }
}