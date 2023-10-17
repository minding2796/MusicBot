package org.discord.utils;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;

public class PlayerManager extends DefaultAudioPlayerManager {
	static PlayerManager playerManager;
	private PlayerManager() {
		AudioSourceManagers.registerRemoteSources(this);
		AudioSourceManagers.registerLocalSource(this);
		source(YoutubeAudioSourceManager.class).setPlaylistPageCount(10);
	}
	public static PlayerManager getInstance() {
		if (playerManager == null) playerManager = new PlayerManager();
		return playerManager;
	}
}
