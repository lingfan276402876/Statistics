package uc.game.statistics.utils;

import hirondelle.date4j.DateTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.time.DateUtils;

public class TimeUtils {
    /**
     * 获取当前属于一年中的第几天
     * @return
     */
	public static int getDayOfYear() {
		return getDayOfYear(System.currentTimeMillis());
	}
	 /**
     * 获取指定时间属于一年中的第几天
     * @return
     */
	public static int getDayOfYear(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		return calendar.get(Calendar.DAY_OF_YEAR);
	}
	/**
	 * 获取当前属于一年中的第几周
	 * @return
	 */
	public static int getWeekOfYear() {
		return getWeekOfYear(System.currentTimeMillis());
	}
	/**
	 * 获取指定时间属于一年中的第几周
	 * @return
	 */
	public static int getWeekOfYear(long curretTime) {
		int currentWeek = 0;
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(curretTime);
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {//星期天为当前周-1
			currentWeek = calendar.get(Calendar.WEEK_OF_YEAR) - 1; // 当前周
		} else {
			currentWeek = calendar.get(Calendar.WEEK_OF_YEAR); // 当前周
		}
		if (currentWeek == 0) {
			currentWeek = 52;
		}
		return currentWeek;
	}
    /**
     * 判断指定时间是否和当前时间是同一天
     * @param dateTime
     * @return
     */
	public static boolean isToday(long dateTime) {
		boolean same = DateUtils.isSameDay(new Date(), new Date(dateTime));
		return same;
	}
    /**
     * 判断指定时间是否和当前时间是同一天
     * @param date
     * @return
     */
	public static boolean isToday(Date date) {
		if (date == null)
			return false;
		return DateUtils.isSameDay(new Date(), date);
	}
    /**
     * 是否为星期天
     * @return
     */
	public static boolean isSunday() {
		Calendar calendar = Calendar.getInstance();
		return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
	}

	/**
	 * 判断是否为周末
	 * 
	 * @return
	 */
	public static boolean isWeekend() {
		Calendar calendar = Calendar.getInstance();
		return (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY);
	}
    /**
     * 将毫秒转换成指定的时间格式
     * @param m 毫秒
     * @param pattern 时间格式
     * @return
     */
	public static String formatDate(long m,String pattern) {
		Date date = new Date(m);
		SimpleDateFormat df = new SimpleDateFormat(pattern);
		String year_month = df.format(date);
		return year_month;
	}
	/**
	 * 将时间前移或者后移多少天
	 * @param date 时间
	 * @param pattern 时间格式
	 * @param day 天数
	 * @return
	 */
	public static String getNewFormatDate(String date,String pattern,int day) {
		SimpleDateFormat df = new SimpleDateFormat(pattern);
		Date d = null;
		try {
			d = df.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(d);
		calendar.add(Calendar.DAY_OF_YEAR, day);
		return df.format(calendar.getTime());
	}
	/**
	 * 将时间字符串转化成毫秒
	 * @param dateTime 时间字符串
	 * @param pattern 时间格式
	 * @return
	 */
	public static long getMilliseconds(String dateTime,String pattern) {
		SimpleDateFormat df = new SimpleDateFormat(pattern);
		try {
			return df.parse(dateTime).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}
	/**
	 * 是否在时间段范围内
	 * 
	 * @return
	 */
	public static boolean isPreActivity(String startTime, String endTime,String pattern) {
		SimpleDateFormat df = new SimpleDateFormat(pattern);
		TimeZone timeZone = TimeZone.getDefault();
		DateTime now = DateTime.now(timeZone);
		DateTime startDate = new DateTime(df.format(new Date()).concat(startTime));
		DateTime endDate = new DateTime(df.format(new Date()).concat(endTime));
		if (now.gteq(startDate) && now.lt(endDate)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isPreActivity(String startTime[], String[] endTime,String pattern) {
		for (int i = 0; i < startTime.length; i++) {
			String start = startTime[i];
			String end = endTime[i];
			if (isPreActivity(start, end, pattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断当前是时间是否再指定星期几 的某个时间段内
	 * 
	 * @param dayofweek
	 *            星期几数组
	 * @param startTime
	 *            开始时间数组
	 * @param endTime
	 *            结束时间数组
	 * @return
	 */
	public static boolean isPreActivity(String[] dayofweek, String startTime[], String[] endTime,String pattern) {
		int weekday = getCurrentWeenkDay();
		boolean flag = false;
		for (String dw : dayofweek) {
			if (Integer.parseInt(dw) == weekday) {
				flag = true;
				break;
			}
		}
		if (!flag) {
			return false;
		}
		for (int i = 0; i < startTime.length; i++) {
			String start = startTime[i];
			String end = endTime[i];
			if (isPreActivity(start, end, pattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断当前是时间是否再指定星期几内
	 * 
	 * @param dayofweek
	 *            星期几数组
	 * @return
	 */
	public static boolean isPreActivity(String[] dayofweek) {
		int weekday = getCurrentWeenkDay();
		boolean flag = false;
		for (String dw : dayofweek) {
			if (Integer.parseInt(dw) == weekday) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	/**
	 * 获取中国日历的星期几
	 * 
	 * @return
	 */
	public static int getCurrentWeenkDay() {
		TimeZone timeZone = TimeZone.getDefault();
		DateTime now = DateTime.now(timeZone);
		int weekday = now.getWeekDay();
		if (weekday == 1)
			return 7;
		return weekday - 1;
	}

	/**
	 * 获取两个时间之间相差天数
	 * 
	 * @param firstDate
	 *            开始的时间
	 * @param secondDate
	 *            结束时间
	 * @return
	 */
	public static int getTimeDifference(Date firstDate, Date secondDate) {
		Calendar first = Calendar.getInstance();
		first.setTime(firstDate);
		Calendar second = Calendar.getInstance();
		second.setTime(secondDate);
		return second.get(Calendar.DAY_OF_YEAR) - first.get(Calendar.DAY_OF_YEAR);
	}

	/**
	 * 格式化时分秒
	 * 
	 * @param hms
	 * @return
	 */
	public static Calendar formatHMS(String hms) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String date_str = df.format(date) + " ";
		String time = date_str.concat(hms);
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(df.parse(time));
			return calendar;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 格式化时分秒
	 * 
	 * @param hms
	 * @return
	 */
	public static String getHMS(Calendar calendar) {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		return df.format(calendar.getTime());
	}

	/**
	 * 获取当前时间和整点相差的秒数
	 * 
	 * @return
	 */
	public static long getDifferenceSecondToZDTIME() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		return (calendar.getTimeInMillis() - System.currentTimeMillis()) / 1000l;

	}
}
