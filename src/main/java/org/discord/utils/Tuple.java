package org.discord.utils;

public record Tuple<A, B>(A a, B b) {
	@Override
	public String toString() {
		return a + ", " + b;
	}
	
	public static Tuple<String, String> fromString(String tuple) {
		String a, b;
		a = tuple.split(", ")[0];
		b = tuple.split(", ")[1];
		return new Tuple<>(a, b);
	}
}
