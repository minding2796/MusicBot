package org.discord.utils;

import java.util.Map;

public class MapUtils {
	public static <K, V> K getByFirstValue(Map<K, V> map, V value) {
		return getByFirstValueOrDefault(map, value, null);
	}
	public static <K, V> K getByFirstValueOrDefault(Map<K, V> map, V value, K defaultValue) {
		for (Map.Entry<K, V> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return defaultValue;
	}
}
