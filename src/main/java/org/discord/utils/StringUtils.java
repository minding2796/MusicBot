package org.discord.utils;

import java.util.ArrayList;
import java.util.List;

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
	
	public static long fromParsedUnixtime(String time) {
		String[] a = time.split(":");
		long unixtime = 0L;
		if (a.length > 3) throw new IllegalArgumentException();
		if (a.length == 3) {
			unixtime += Long.parseLong(a[0]) * 3600;
			unixtime += Long.parseLong(a[1]) * 60;
			unixtime += Long.parseLong(a[2]);
		} else if (a.length == 2) {
			unixtime += Long.parseLong(a[0]) * 60;
			unixtime += Long.parseLong(a[1]);
		} else {
			unixtime += Long.parseLong(a[0]);
		}
		return unixtime * 1000;
	}
	
	public static List<Integer> parseRange(String range) {
		List<Integer> result = new ArrayList<>();
		String[] args = range.split(",");
		for (String arg : args) {
			String[] r = arg.trim().split("~");
			if (r.length == 1) {
				Integer p = Integer.parseInt(r[0].trim());
				if (!result.contains(p)) result.add(p);
				continue;
			}
			if (r.length == 2) {
				int p1 = Integer.parseInt(r[0].trim());
				Integer p2 = Integer.parseInt(r[1].trim());
				for (Integer i = p1; i.compareTo(p2) < 1; i = i + 1) {
					if (!result.contains(i)) result.add(i);
				}
				continue;
			}
			throw new IllegalArgumentException();
		}
		return result;
	}
}
