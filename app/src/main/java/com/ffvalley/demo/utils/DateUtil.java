package com.ffvalley.demo.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static java.util.Calendar.getInstance;

public class DateUtil {
    private static final String TAG = "DateUtil";

    private static Date changeTimeZone(Date date) {
        TimeZone oldZone = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone newZone = TimeZone.getTimeZone("GMT");
        Date date_ = changeTimeZone(date, oldZone, newZone);
        return date_;
    }

    private static Date changeTimeZone(Date date, TimeZone oldZone, TimeZone newZone) {
        Date date_ = null;
        if (date != null) {
            int timeOffset = oldZone.getRawOffset() - newZone.getRawOffset();
            date_ = new Date(date.getTime() - timeOffset);
        }
        return date_;
    }

    /**
     * 获取本周指定第几天的日期
     *
     * @param dayWeek 范围0-6
     */
    public static int getTheSpecialWeekDay(int dayWeek) {
        if (dayWeek < 0 || dayWeek > 6) return 0;
        int specialDay;
        Calendar cd = getInstance();
        int dayOfWeek = cd.get(Calendar.DAY_OF_WEEK) - 1; // 因为按中国礼拜一作为第一天所以这里减1
        if (dayOfWeek == 1) {
            specialDay = 0;
        } else {
            specialDay = 1 - dayOfWeek;
        }
        specialDay = specialDay + dayWeek;

        GregorianCalendar currentDate = new GregorianCalendar();
        currentDate.add(GregorianCalendar.DATE, specialDay);
        Date date = currentDate.getTime();
        Date d = new Date(1970, 0, 1);
        long millions = date.getTime() - d.getTime();
        double seconds = Math.floor(millions / 1000);
        int time = (int) seconds;
//        DateFormat df = DateFormat.getDateInstance();
//        String preDay = df.format(date);
        return time;
    }

    //获取本周的结束时间
    public static Date getEndDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getBeginDayOfWeek());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        Date weekEndSta = cal.getTime();
        return getDayEndTime(weekEndSta);
    }

    //获取本周的开始时间
    public static Date getBeginDayOfWeek() {
        Date date = new Date();
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayofweek == 1) {
            dayofweek += 7;
        }
        cal.add(Calendar.DATE, 2 - dayofweek);
        return getDayStartTime(cal.getTime());
    }

    //获取某个日期的开始时间
    public static Timestamp getDayStartTime(Date d) {
        Calendar calendar = Calendar.getInstance();
        if (null != d) calendar.setTime(d);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTimeInMillis());
    }

    //获取某个日期的结束时间
    public static Timestamp getDayEndTime(Date d) {
        Calendar calendar = Calendar.getInstance();
        if (null != d) calendar.setTime(d);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return new Timestamp(calendar.getTimeInMillis());
    }


    /**
     * 获取本月第一天的秒值
     */
    public static int getMonthFirstDay(Calendar calendar) {
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date date = calendar.getTime();
        Log.d(TAG, "本月第一天是:" + date.toString());
        long seconds = date.getTime() / 1000L;
        return (int) seconds;
    }

    /**
     * 获取本月最后一天的秒值
     */
    public static int getMonthLastDay(Calendar calendar) {
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        Date date = calendar.getTime();
        Log.d(TAG, "本月最后一天是:" + date.toString());
        long seconds = date.getTime() / 1000L;
        return (int) seconds;
    }

    //获取本月的开始时间
    public static Date getBeginDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(getNowYear(), getNowMonth() - 1, 1);
        return getDayStartTime(calendar.getTime());
    }

    //获取本月的结束时间
    public static Date getEndDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(getNowYear(), getNowMonth() - 1, 1);
        int day = calendar.getActualMaximum(5);
        calendar.set(getNowYear(), getNowMonth() - 1, day);
        return getDayEndTime(calendar.getTime());
    }

    //获取今年是哪一年
    public static Integer getNowYear() {
        Date date = new Date();
        GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
        gc.setTime(date);
        return Integer.valueOf(gc.get(1));
    }

    //获取本月是哪一月
    public static int getNowMonth() {
        Date date = new Date();
        GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
        gc.setTime(date);
        return gc.get(2) + 1;
    }

    //获取月日 和星期
    public static Date getDate(int time) {
        Date date = new Date(time * 1000L);
        date = changeTimeZone(date);
        return date;
    }

    public static Date getDateZWJ(int time) {
        Date date = new Date(time * 1000L + 8 * 60 * 60 * 1000);
        date = changeTimeZone(date);
        return date;
    }

    //获取月日 和星期
    public static String getMMDDEE(int time) {
        Date date = getDate(time);
        SimpleDateFormat f4 = new SimpleDateFormat("MM月dd日 E");
        return f4.format(date);
    }

    //获取时分
    public static String getHHMM(int time) {
        Date date = getDate(time);
        SimpleDateFormat f4 = new SimpleDateFormat("HH:mm");
        return f4.format(date);
    }

    //获取时分秒
    @SuppressLint("DefaultLocale")
    public static String getHHMMSS(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        return hours > 0 ? String.format("%02d:%02d:%02ds", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    //获取时分秒
    public static String getHHMMSS(Date date) {
        SimpleDateFormat f4 = new SimpleDateFormat("HH:mm:ss");
        return f4.format(date);
    }

    //获取年月日
    public static String getYYMMDD(Date date) {

        SimpleDateFormat f4 = new SimpleDateFormat("yyyy/MM/dd  E");
        return f4.format(date);
    }

    public static String getYYMMDDI(int time) {
        Date date = getDate(time);
        SimpleDateFormat f4 = new SimpleDateFormat("yyyy年MM月dd日");
        return f4.format(date);
    }

    //获取年月日时分
    public static String getYYMMDDHHMM(Date date) {
        SimpleDateFormat f4 = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
        return f4.format(date);
    }

    public static String getYYMMDDHHMMNO(Date date) {
        SimpleDateFormat f4 = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
        return f4.format(date);
    }

    public static String getYYMMDDHHMMD(Date date) {
        Date date1 = changeTimeZone(date);
        SimpleDateFormat f4 = new SimpleDateFormat("yyyy.MM.dd  HH:mm");
        return f4.format(date1);
    }

    /**
     * 判断当前时间是否结束时间内
     *
     * @param endTime 结束时间
     */
    public static boolean isCurrentTimeDuration(int endTime) {
        boolean isDuration = false;
        long currentSeconds = System.currentTimeMillis() / 1000L;
        if (currentSeconds <= endTime) isDuration = true;
        return isDuration;
    }

}
