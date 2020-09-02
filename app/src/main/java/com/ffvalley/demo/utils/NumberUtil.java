package com.ffvalley.demo.utils;

import java.text.NumberFormat;
import java.util.regex.Pattern;

public class NumberUtil {

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    // 获取比率值
    public static String ratio(long numerator, long denominator, int multiple) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format((float) numerator / (float) denominator * multiple).replace(",", "");
    }

}
