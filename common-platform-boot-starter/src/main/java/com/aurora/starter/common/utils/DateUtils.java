package com.aurora.starter.common.utils;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

/**
 * 时间工具类
 *
 * @author author
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    public static final String YYYY = "yyyy";

    public static final String YYYY_MM = "yyyy-MM";

    public static final String MONTH_DATE_PATTERN = "yyyyMM";

    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private static final String[] parsePatterns = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM",
            "yyyy年MM月dd日"};

    /**
     * 获取当前Date型日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate() {
        return new Date();
    }

    public static Date yesterday() {
        return offsetDay(getNowDate(), -1);
    }

    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     *
     * @return String
     */
    public static String getDate() {
        return dateTimeNow(YYYY_MM_DD);
    }

    public static String getTime() {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static String dateTimeNow() {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static String dateTimeNow(final String format) {
        return parseDateToStr(format, new Date());
    }

    public static String dateTime(final Date date) {
        return parseDateToStr(YYYY_MM_DD, date);
    }

    public static String parseDateToStr(final String format, final Date date) {
        return new SimpleDateFormat(format).format(date);
    }

    public static Date dateTime(final String format, final String ts) {
        try {
            return new SimpleDateFormat(format).parse(ts);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static String datePath() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyy/MM/dd");
    }

    /**
     * 日期路径 即年/月/日 如20180808
     */
    public static String dateTime() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyyMMdd");
    }

    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return parseDate(str.toString(), parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate() {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算相差天数
     */
    public static int differentDaysByMillisecond(Date date1, Date date2) {
        return Math.abs((int) ((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24)));
    }

    /**
     * 计算两个时间差
     */
    public static String getDatePoor(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 增加 LocalDateTime ==> Date
     */
    public static Date toDate(LocalDateTime temporalAccessor) {
        if (Objects.isNull(temporalAccessor)) {
            return null;
        }
        ZonedDateTime zdt = temporalAccessor.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 增加 LocalDate ==> Date
     */
    public static Date toDate(LocalDate temporalAccessor) {
        LocalDateTime localDateTime = LocalDateTime.of(temporalAccessor, LocalTime.of(0, 0, 0));
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 增加 Date ==> LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 获取年份之间的月份.
     *
     * @param startDate startDate
     * @param endDate   endDate
     * @return 结果
     */
    public static List<YearMonth> getYearMonthsBetween(final Date startDate, final Date endDate) {
        List<YearMonth> yearMonths = new ArrayList<>();
        // 日期为空
        if (Objects.isNull(startDate) || Objects.isNull(endDate)) {
            return yearMonths;
        }
        // 开始时间不能在结束时间之后
        if (startDate.after(endDate)) {
            throw new RuntimeException("开始时间不能在结束时间之后");
        }

        // 日期转换为LocalDate
        LocalDate localStartDate = DateUtils.toLocalDate(startDate);
        LocalDate localEndDate = DateUtils.toLocalDate(endDate);

        // 将当前日期设置为每月的第一天
        LocalDate currentDate = localStartDate.withDayOfMonth(1);

        // 判断当前日期是否超过结束日期
        while (!currentDate.isAfter(localEndDate)) {
            yearMonths.add(YearMonth.from(currentDate));
            // 加上一个月
            currentDate = currentDate.plusMonths(1);
        }

        return yearMonths;
    }

    public static Date beginOfDay(final Date date) {
        Calendar time = Calendar.getInstance();
        time.setTime(date);
        time.set(Calendar.HOUR_OF_DAY, 0);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTime();
    }

    public static Date endOfDay(final Date date) {
        Calendar time = Calendar.getInstance();
        time.setTime(date);
        time.set(Calendar.HOUR_OF_DAY, 23);
        time.set(Calendar.MINUTE, 59);
        time.set(Calendar.SECOND, 59);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTime();
    }

    public static Date beginOfHour(final Date date) {
        Calendar time = Calendar.getInstance();
        time.setTime(date);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTime();
    }

    public static Date endOfHour(final Date date) {
        Calendar time = Calendar.getInstance();
        time.setTime(date);
        time.set(Calendar.MINUTE, 59);
        time.set(Calendar.SECOND, 59);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTime();
    }

    public static Date offsetDay(final Date date, int offset) {
        return offset(date, Calendar.DAY_OF_YEAR, offset);
    }

    public static Date offsetHour(final Date date, int offset) {
        return offset(date, Calendar.HOUR_OF_DAY, offset);
    }

    public static Date offset(final Date date, int field, int offset) {
        Calendar time = Calendar.getInstance();
        time.setTime(date);
        time.add(field, offset);
        return time.getTime();
    }

    /**
     * 获取时间
     *
     * @param hour 24小时进制
     * @return
     */
    public static Date getDayTime(int hour, int minute, int second, int day) {
        Calendar time = Calendar.getInstance();
        if (day != 0) {
            time.add(Calendar.DATE, 1);
        }
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, second);
        return time.getTime();

    }

    /**
     * 清除日期的毫秒数（毫秒数大于500后存入sql会进位，因此需要清除）
     * @param date 时间
     * @return 清除毫秒数后的结果
     */
    public static Date clearMillisecond(Date date) {
        Calendar time = Calendar.getInstance();
        time.setTime(date);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTime();

    }

    /**
     * 根据时间 和时间格式 校验是否正确
     * @param length 校验的长度
     * @param sDate 校验的日期
     * @param format 校验的格式
     * @return
     */
    public static boolean isLegalDate(int length, String sDate,String format) {
        int legalLen = length;
        if ((sDate == null) || (sDate.length() != legalLen)) {
            return false;
        }
        DateFormat formatter = new SimpleDateFormat(format);
        try {
            Date date = formatter.parse(sDate);
            return sDate.equals(formatter.format(date));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算两个时间相差是否在指定时间量内
     *
     * @param start 开始时间
     * @param end   结束时间
     * @param amount   指定时间量
     * @param field   时间量单位 如java.util.Calendar.MONTH
     * @return 对比结果 true是在指定时间量内，false为不在指定时间量内
     */
    public static boolean calculationMonth(Date start, Date end, int amount, int field) {
        if (start.after(end)) {
            Date t = start;
            start = end;
            end = t;
        }
        Calendar c1 = Calendar.getInstance();
        c1.setTime(start);
        c1.add(field, amount);
        Date time = c1.getTime();
        return time.before(end) || time.equals(end);
    }
}
