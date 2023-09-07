package org.discord.utils;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class YTSourceManager extends YoutubeAudioSourceManager {
	public YTSourceManager() {
		super();
	}
	public YTSourceManager(boolean allowSearch) {
		super(allowSearch);
	}
	
	public AudioItem playlist(String playlistId, String selectedVideoId) {
		try {
			Field field = YoutubeAudioSourceManager.class.getDeclaredField("loadingRoutes");
			Method method = YoutubeAudioSourceManager.class.getDeclaredClasses()[0].getDeclaredMethod("playlist", String.class, String.class);
			method.setAccessible(true);
			field.setAccessible(true);
			return (AudioItem) method.invoke(field.get(this), playlistId, selectedVideoId);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
	
	public AudioItem search(String query) {
		try {
			Field field = YoutubeAudioSourceManager.class.getDeclaredField("loadingRoutes");
			Method method = YoutubeAudioSourceManager.class.getDeclaredClasses()[0].getDeclaredMethod("search", String.class);
			method.setAccessible(true);
			field.setAccessible(true);
			return (AudioItem) method.invoke(field.get(this), query);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
