package com.hongshu.common.utils;
/*
 * 时间操作工具类
 *
 * @Author hongshu
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class DateUtilss {

    public static final String STARTTIME = " 00:00:00";
    public static final String ENDTIME = " 23:59:59";
    public final static String FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";
    public final static String[] REPLACE_STRING = new String[]{"GMT+0800", "GMT+08:00"};
    public final static String SPLIT_STRING = "(中国标准时间)";
    public static final Logger log = LoggerFactory.getLogger(DateUtilss.class);

    public static String YYYY = "yyyy";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private DateUtilss() {
    }

    /**
     * 获取现在的时间 yyyy-MM-dd HH:mm:ss
     *
     */
    public static String getNowTime() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return formate.format(date);
    }

    /**
     * 获取当前Date型日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate() {
        return new Date();
    }

    /**
     * @author hongshu
     * 创建时间：2018年6月14日
     */
    public static String getNowTimeFormat() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date = new Date(System.currentTimeMillis());
        return formate.format(date);
    }

    public static Date str2Date(String dateString) {
        try {
            dateString = dateString.split(Pattern.quote(SPLIT_STRING))[0].replace(REPLACE_STRING[0], REPLACE_STRING[1]);
            SimpleDateFormat sf1 = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss z", Locale.US);
            return sf1.parse(dateString);
        } catch (Exception e) {
            throw new RuntimeException("时间转化格式错误" + "[dateString=" + dateString + "]" + "[FORMAT_STRING=" + FORMAT_STRING + "]");
        }
    }

    /**
     * 获取今天开始的时间
     *
     */
    public static String getToDayStartTime() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        Date date = new Date(System.currentTimeMillis());
        return formate.format(date);
    }

    /**
     * 获取今天结束的时间
     *
     */
    public static String getToDayEndTime() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        Date date = new Date(System.currentTimeMillis());
        return formate.format(date);
    }

    /**
     * 获取昨天开始的时间
     *
     */
    public static String getYestodayStartTime() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        Date date = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        return formate.format(date);
    }

    /**
     * 获取昨天结束的时间
     *
     */
    public static String getYestodayEndTime() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        Date date = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        return formate.format(date);
    }

    private static Date parseDay(String value) {
        Date date = DateUtils.parseDate(value);
        if (date != null) {
            return date;
        }
        try {
            return org.apache.commons.lang3.time.DateUtils.parseDate(value, Locale.US,
                    "EEE MMM dd HH:mm:ss zzz yyyy", "EEE, dd MMM yyyy HH:mm:ss zzz",
                    "E MMM dd yyyy HH:mm:ss z", "MM/dd/yyyy", "MMM dd yyyy");
        } catch (ParseException e) {
            throw new IllegalArgumentException("无法解析日期: " + value, e);
        }
    }

    /**
     * 获取某天开始的时间
     *
     */
    public static String getOneDayStartTime(String oneDay) {
        return getOneDayStartTime(parseDay(oneDay));
    }

    /**
     * 获取某天开始的日期
     *
     */
    public static String getOneDayStartTime(Date oneDay) {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        return formate.format(oneDay);
    }

    /**
     * 获取某天结束的时间
     *
     */
    public static String getOneDayEndTime(String oneDay) {
        return getOneDayStartTime(parseDay(oneDay));
    }

    /**
     * 获取某天结束的日期
     *
     */
    public static String getOneDayEndTime(Date oneDay) {
        return getOneDayStartTime(oneDay);
    }

    /**
     * 获取本周开始的时间
     *
     */
    public static Date getWeekStartTime() {
        // 获得本周一0点时间
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return cal.getTime();
    }

    /**
     * 将  String 转换成  date
     *
     */
    public static Date strToDateTime(String dateTime) {
        Date date = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = format.parse(dateTime);
        } catch (ParseException e) {
            org.slf4j.LoggerFactory.getLogger(DateUtilss.class).warn("操作失败", e);
        }
        return date;
    }

    /**
     * 将  date 转换成  时间戳
     *
     */
    public static Long dateToStamp(String s) throws ParseException {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        return date.getTime();
    }

    /**
     * Date 转换成  String
     *
     */
    public static String dateTimeToStr(Date dateTime) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(dateTime);
    }

    /**
     * 获取本周开始的时间的字符串
     *
     */
    public static String getWeekStartTimeStr() {
        // 获得本周一0点时间
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        return formate.format(cal.getTime());
    }

    /**
     * 获取本周结束的时间
     *
     */
    public static Date getWeekEndTime() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getWeekStartTime());
        cal.add(Calendar.DAY_OF_WEEK, 7);
        return cal.getTime();
    }

    /**
     * 获取本周结束的时间的字符串
     *
     */
    public static String getWeekEndTimeStr() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getWeekStartTime());
        cal.add(Calendar.DAY_OF_WEEK, 7);
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        return formate.format(cal.getTime());
    }

    /**
     * 获取上周开始的时间的字符串
     *
     */
    public static String getLastWeekStartTimeStr() {
        int weeks = -1;
        int mondayPlus = getMondayPlus();
        GregorianCalendar currentDate = new GregorianCalendar();
        currentDate.add(GregorianCalendar.DATE, mondayPlus + 7 * weeks);
        Date monday = currentDate.getTime();
        return getOneDayStartTime(monday);
    }

    /**
     * 获取本月开始的时间
     *
     */
    public static Date getMonthStartTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }

    /**
     * 获取本月开始的时间的字符串
     *
     */
    public static String getMonthStartTimeStr() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        return formate.format(getMonthStartTime());
    }

    /**
     * 获取本月结束的时间
     *
     */
    public static Date getMonthEndTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 24);
        return cal.getTime();
    }

    /**
     * 获取本月结束的时间的字符串
     *
     */
    public static String getMonthEndTimeStr() {
        SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        return formate.format(getMonthEndTime());
    }

    /**
     * 获取当月的 天数
     */
    public static int getCurrentMonthDay() {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        return a.get(Calendar.DATE);
    }

    /**
     * 得到二个日期间的间隔天数
     *
     */
    public static int getDayByTwoDay(String date1, String date2) {
        SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
        long day;
        try {
            Date date = myFormatter.parse(date1);
            Date mydate = myFormatter.parse(date2);
            day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return 0;
        }
        return (int) day;
    }

    /**
     * 得到两个日期相差的秒数
     *
     */
    public static int getSecondByTwoDay(Date lastDate, Date date) {
        long second;
        try {
            second = (lastDate.getTime() - date.getTime()) / 1000;
        } catch (Exception e) {
            return 0;
        }
        return (int) second;
    }

    /**
     * 判断某个日期属于本周的第几天 (星期一代表第一天)
     *
     */
    public static int getDaysByWeek(String dateTime) throws ParseException {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = dateFormat.parse(dateTime);
        cal.setTime(date);
        int day = cal.get(Calendar.DAY_OF_WEEK);
        day = day - 1;
        if (day == 0) {
            day = 7;
        }
        return day;
    }

    /**
     * 判断某个日期属于本月的第几天
     *
     */
    public static int getDaysByMonth(String dateTime) throws ParseException {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = dateFormat.parse(dateTime);
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 根据年 月 获取对应的月份 天数
     */
    public static int getDaysByYearMonth(int year, int month) {

        Calendar a = Calendar.getInstance();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month - 1);
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        return a.get(Calendar.DATE);
    }
}
