package org.discord.utils;

public class StringUtils {
	public static long count(String input, char... selector) {
		long count = 0;
		char[] s = input.toCharArray();
		for (char a : s) {
			for (char z : selector) {
				if (z == a) count++;
			}
		}
		return count;
	}
	public static String replaceMarkdown(String s) {
		return s.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~").replace("|", "\\|").replace(">", "\\>").replace("`", "\\`");
	}
	public static String parseUnixTime(long time, boolean showHour) {
		time /= 1000;
		String second = String.valueOf(time % 60);
		time /= 60;
		String minute = String.valueOf(time % 60);
		time /= 60;
		String hour = String.valueOf(time);
		if (hour.equalsIgnoreCase("0") && !showHour) {
			if (minute.length() == 1) {
				minute = "0" + minute;
			}
			if (second.length() == 1) {
				second = "0" + second;
			}
			return minute + ":" + second;
		} else {
			if (hour.length() == 1) {
				hour = "0" + hour;
			}
			if (minute.length() == 1) {
				minute = "0" + minute;
			}
			if (second.length() == 1) {
				second = "0" + second;
			}
			return hour + ":" + minute + ":" + second;
		}
	}
}
