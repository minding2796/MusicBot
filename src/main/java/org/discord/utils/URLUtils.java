package org.discord.utils;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("deprecation")
public class URLUtils {
	public static Tuple<String, String> getVideoId(String url) throws MalformedURLException, UnsupportedOperationException, IllegalArgumentException {
		if (isPlaylist(url)) return new Tuple<>(null, url);
		if (isVideoId(url)) return new Tuple<>(url, null);
		if (!isURL(url)) throw new MalformedURLException("Invalid URL: " + url);
		if (!url.startsWith("https://")) throw new UnsupportedOperationException("only https URL is supported");
		url = url.substring(8);
		String[] param = url.split("/");
		if (param.length < 2) throw new IllegalArgumentException("Video ID not Found");
		if (!param[1].contains("?") && !param[1].contains("=") && !param[1].contains("&")) return new Tuple<>(param[1], null);
		if (param[1].split("[?]").length <= 1) return new Tuple<>(param[1], null);
		String id = null, list = null;
		if (!param[1].split("[?]")[0].equalsIgnoreCase("watch") && !param[1].split("[?]")[0].equalsIgnoreCase("playlist")) id = param[1].split("[?]")[0];
		String[] ts = param[1].split("[?]")[1].split("&");
		for(String s : ts) {
			String[] v = s.split("=");
			if (id == null) {
				if (v[0].equalsIgnoreCase("v")) {
					id = v[1];
				} else if (v[0].equalsIgnoreCase("list")) {
					list = v[1];
				}
			} else {
				if (v[0].equalsIgnoreCase("list")) {
					list = v[1];
				}
			}
		}
		return new Tuple<>(id, list);
	}
	
	public static Tuple<String, String> getVideoIdNoThrow(String url) {
		if (isPlaylist(url)) return new Tuple<>(url, null);
		if (isVideoId(url)) return new Tuple<>(url, null);
		url = url.substring(8);
		String[] param = url.split("/");
		if (!param[1].contains("?") && !param[1].contains("=") && !param[1].contains("&")) return new Tuple<>(param[1], null);
		if (param[1].split("[?]").length <= 1) return new Tuple<>(param[1], null);
		String id = null, list = null;
		if (!param[1].split("[?]")[0].equalsIgnoreCase("watch")) id = param[1].split("[?]")[0];
		String[] ts = param[1].split("[?]")[1].split("&");
		for(String s : ts) {
			String[] v = s.split("=");
			if (id == null) {
				if (v[0].equalsIgnoreCase("v")) {
					url = v[1];
				} else if (v[0].equalsIgnoreCase("list")) {
					list = v[1];
				}
			} else {
				if (v[0].equalsIgnoreCase("list")) {
					list = v[1];
				}
			}
		}
		return new Tuple<>(url, list);
	}
	
	public static boolean isURL(String url) {
		try {
			URL con = new URL(url);
			con.openConnection().connect();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	public static boolean isPlaylist(String url) {
		if (url.contains(":") || url.contains("/") || url.contains("?") || url.contains("&") || url.contains("=")) return false;
		try {
			BasicAudioPlaylist playlist = (BasicAudioPlaylist) new YTSourceManager().playlist(url, null);
			return !playlist.getTracks().isEmpty();
		} catch (Throwable e) {
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
