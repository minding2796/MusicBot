package org.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Message;
import org.discord.utils.Tuple;

public class ErrorResultHandler implements AudioLoadResultHandler {
	TrackEndEvent e;
	Message m;
	public ErrorResultHandler(TrackEndEvent e, Message m) {
		this.e = e;
		this.m = m;
	}
	@Override
	public void trackLoaded(AudioTrack item) {
		var list = MusicBot.queue.remove(m.getGuild());
		list.set(0, new Tuple<>(item, m));
		MusicBot.queue.put(m.getGuild(), list);
		e.player.playTrack(item);
	}
	
	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
	}
	
	@Override
	public void noMatches() {
	}
	
	@Override
	public void loadFailed(FriendlyException exception) {
	}
}
