package org.discord.utils;

public class ArrayUtils {
	public static boolean contains(char[] arr, char value) {
		for (char c : arr) {
			if (c == value) return true;
		}
		return false;
	}
	public static <T> boolean contains(T[] arr, T value) {
		for (T t : arr) {
			if (t == value) return true;
		}
		return false;
	}
}
