package org.discord.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtils {
	public static String readFile(File file) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
		String line;
		StringBuilder result = new StringBuilder();
		while ((line = bufferedReader.readLine()) != null) {
			result.append(line).append("\n");
		}
		if (result.isEmpty()) return "";
		return result.substring(0, result.length() - 1);
	}
	public static void writeFile(File file, String content) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
		writer.write(content);
		writer.flush();
		writer.close();
	}
}
