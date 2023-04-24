package org.discord.utils;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class URLUtils {
	public static String getVideoId(String url) throws MalformedURLException, UnsupportedOperationException, IllegalArgumentException {
		if (isVideoId(url)) return url;
		if (!isURL(url)) throw new MalformedURLException("Invalid URL: " + url);
		if (!url.startsWith("https://")) throw new UnsupportedOperationException("only https URL is supported");
		url = url.substring(8);
		String[] param = url.split("/");
		if (param.length < 2) throw new IllegalArgumentException("Video ID not Found");
		if (!param[1].contains("?") && !param[1].contains("=") && !param[1].contains("&")) return param[1];
		if (!param[1].split("[?]")[0].equalsIgnoreCase("watch")) return param[1];
		String[] ts = param[1].split("[?]")[1].split("&");
		for(String s : ts) {
			String[] v = s.split("=");
			if (!v[0].equalsIgnoreCase("v")) continue;
			url = v[1];
		}
		return url;
	}
	
	public static String getVideoIdNoThrow(String url) {
		if (isVideoId(url)) return url;
		url = url.substring(8);
		String[] param = url.split("/");
		if (!param[1].contains("?") && !param[1].contains("=") && !param[1].contains("&")) return param[1];
		String[] ts = param[1].split("[?]")[1].split("&");
		for(String s : ts) {
			String[] v = s.split("=");
			if (!v[0].equalsIgnoreCase("v")) continue;
			url = v[1];
		}
		return url;
	}
	
	public static boolean isURL(String url) {
		try {
			var con = new URL(url);
			con.openConnection().connect();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	public static boolean isVideoId(String url) {
		try {
			new YoutubeAudioSourceManager().loadTrackWithVideoId(url, true);
			return true;
		} catch (FriendlyException e) {
			return false;
		}
	}
}
