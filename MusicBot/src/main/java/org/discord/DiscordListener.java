package org.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.AudioManager;
import org.discord.utils.AudioPlayerSendHandler;
import org.discord.utils.StringUtils;
import org.discord.utils.URLUtils;

import java.awt.*;
import java.net.MalformedURLException;
import java.util.ArrayList;

import static org.discord.MusicBot.*;

public class DiscordListener implements EventListener {
	@Override
	public void onEvent(GenericEvent e) {
		if (e instanceof MessageReceivedEvent event) {
			User u = event.getAuthor();
			if (u.isBot()) return;
			if (u.isSystem()) return;
			Message message = event.getMessage();
			if (message.getContentRaw().equals(commandPrefix + "songrepeat") || message.getContentRaw().equals(commandPrefix + "sr")) {
				message.addReaction(Emoji.fromUnicode("✅")).queue();
				if (repeat.contains(event.getGuild())) {
					repeat.remove(event.getGuild());
				} else {
					repeat.add(event.getGuild());
				}
				return;
			}
			if (message.getContentRaw().equals(commandPrefix + "commands")) {
				message.addReaction(Emoji.fromUnicode("✅")).queue();
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle(":scroll: 명령어 리스트");
				builder.setDescription("""
                        ----- 기본 기능 -----
						%1$scommands : 이 항목을 표시합니다
						%1$sshutdown | %1$s셧다운 : 봇을 종료시킵니다 (서버 주인 전용)
						----- 뮤직봇 -----
						%1$sp | %1$splay : 음악을 재생합니다
						%1$ssr | %1$ssongrepeat : 음악 재생을 반복합니다
						%1$sq | %1$squeue : 현재 재생중인 재생목록을 표시합니다
						%1$snp : 현재 재생중 음악을 표시합니다
						%1$sskip | %1$ssk : 재생중인 음악을 건너뜁니다 (관리자 전용)
						%1$sstop | %1$sst : 현재 재생중인 음악을 강제로 중지합니다 (관리자 전용)
						%1$sremove : 노래를 대기열에서 제거합니다 (관리자 전용)
						""".formatted(commandPrefix));
				builder.setColor(Color.GREEN);
				message.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
				return;
			}
			if (message.getContentRaw().startsWith(commandPrefix + "p ") || message.getContentRaw().startsWith(commandPrefix + "play ")) {
				if (event.getMember().getVoiceState().inAudioChannel()) {
					try {
						int len = 0;
						if (message.getContentRaw().startsWith(commandPrefix + "p ")) len = commandPrefix.length() + 2;
						if (message.getContentRaw().startsWith(commandPrefix + "play ")) len = commandPrefix.length() + 5;
						String videoid = URLUtils.getVideoId(message.getContentRaw().substring(len));
						message.addReaction(Emoji.fromUnicode("✅")).queue();
						var voiceChannel = event.getMember().getVoiceState().getChannel();
						var audioManager = event.getGuild().getAudioManager();
						var youtubeAudioSourceManager = new YoutubeAudioSourceManager();
						var item = (AudioTrack) youtubeAudioSourceManager.loadTrackWithVideoId(videoid, true);
						if (queue.getOrDefault(event.getGuild(), new ArrayList<>()).isEmpty()) {
							var playerManager = new DefaultAudioPlayerManager();
							AudioSourceManagers.registerRemoteSources(playerManager);
							var player = playerManager.createPlayer();
							player.addListener((evt) -> {
								if (evt instanceof TrackEndEvent evnt) {
									if (evnt.endReason.equals(AudioTrackEndReason.LOAD_FAILED)) event.getChannel().sendMessage("유튜브에서 영상을 로드하던 중 오류가 발생했습니다").queue();
									Guild guild = event.getGuild();
									var list = queue.get(guild);
									if (repeat.contains(guild)) {
										AudioTrack track = list.get(0).makeClone();
										track.setPosition(0L);
										list.add(track);
									}
									list.remove(0);
									queue.remove(guild);
									queue.put(guild, list);
									if (!list.isEmpty()) {
										evt.player.playTrack(list.get(0));
									} else {
										guild.getAudioManager().closeAudioConnection();
									}
								}
							});
							var list = new ArrayList<AudioTrack>();
							list.add(item);
							queue.put(event.getGuild(), list);
							player.startTrack(item, false);
							var handler = new AudioPlayerSendHandler(player);
							audioManager.setSelfDeafened(true);
							audioManager.setSendingHandler(handler);
							audioManager.openAudioConnection(voiceChannel);
						} else {
							var list = queue.get(event.getGuild());
							list.add(item);
							queue.put(event.getGuild(), list);
						}
					} catch (MalformedURLException exception) {
						message.addReaction(Emoji.fromUnicode("⛔")).queue();
						message.reply("링크가 URL이 아닙니다").mentionRepliedUser(false).queue();
					} catch (UnsupportedOperationException exception) {
						message.addReaction(Emoji.fromUnicode("⛔")).queue();
						message.reply("https URL만 지원됩니다").mentionRepliedUser(false).queue();
					} catch (IllegalArgumentException exception) {
						message.addReaction(Emoji.fromUnicode("⛔")).queue();
						message.reply("영상 ID를 찾을 수 없습니다").mentionRepliedUser(false).queue();
					}
				} else {
					message.addReaction(Emoji.fromUnicode("⛔")).queue();
					message.reply("오디오 채널에 들어가야 이 명령어를 사용할 수 있습니다").mentionRepliedUser(false).queue();
				}
			}
			if (message.getContentRaw().equals(commandPrefix + "sk") || message.getContentRaw().equals(commandPrefix + "skip")) {
				if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					AudioPlayer p = null;
					if (event.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
						p = hd.audioPlayer;
					}
					if (p != null) {
						message.addReaction(Emoji.fromUnicode("✅")).queue();
						p.stopTrack();
					} else {
						message.addReaction(Emoji.fromUnicode("⛔")).queue();
						message.reply("현재 재생중인 음악이 없습니다").mentionRepliedUser(false).queue();
					}
				} else {
					message.addReaction(Emoji.fromUnicode("⛔")).queue();
					message.reply("음악 재생을 강제종료할 권한이 없습니다").mentionRepliedUser(false).queue();
				}
			}
			if (message.getContentRaw().startsWith(commandPrefix + "remove ")) {
				if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					try {
						int index = Integer.parseInt(message.getContentRaw().substring(commandPrefix.length() + 7));
						if (index == 0) {
							AudioPlayer p = null;
							if (event.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
								p = hd.audioPlayer;
							}
							if (p != null) {
								message.addReaction(Emoji.fromUnicode("✅")).queue();
								p.stopTrack();
							} else {
								message.addReaction(Emoji.fromUnicode("⛔")).queue();
								message.reply("현재 재생중인 음악이 없습니다").mentionRepliedUser(false).queue();
							}
						} else {
							var audiotrack = queue.get(event.getGuild());
							if (audiotrack != null) {
								try {
									audiotrack.remove(index);
									queue.remove(event.getGuild());
									queue.put(event.getGuild(), audiotrack);
									message.addReaction(Emoji.fromUnicode("✅")).queue();
								} catch (IndexOutOfBoundsException exception) {
									message.addReaction(Emoji.fromUnicode("⛔")).queue();
									message.reply("대기열에 없는 번호가 감지되었습니다").mentionRepliedUser(false).queue();
								}
							} else {
								message.addReaction(Emoji.fromUnicode("⛔")).queue();
								message.reply("현재 재생중인 대기열이 없습니다").mentionRepliedUser(false).queue();
							}
						}
					} catch (NumberFormatException exception) {
						message.addReaction(Emoji.fromUnicode("⛔")).queue();
						message.reply("숫자만 입력 가능합니다").mentionRepliedUser(false).queue();
					}
				} else {
					message.addReaction(Emoji.fromUnicode("⛔")).queue();
					message.reply("음악 재생을 강제종료할 권한이 없습니다").mentionRepliedUser(false).queue();
				}
			}
			if (message.getContentRaw().equals(commandPrefix + "st") || message.getContentRaw().equals(commandPrefix + "stop")) {
				if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					message.addReaction(Emoji.fromUnicode("✅")).queue();
					queue.remove(event.getGuild());
					AudioManager audioManager = event.getGuild().getAudioManager();
					audioManager.closeAudioConnection();
				} else {
					message.addReaction(Emoji.fromUnicode("⛔")).queue();
					message.reply("음악 재생을 강제종료할 권한이 없습니다").mentionRepliedUser(false).queue();
				}
			}
			if (message.getContentRaw().equals(commandPrefix + "q") || message.getContentRaw().equals(commandPrefix + "queue")) {
				message.addReaction(Emoji.fromUnicode("✅")).queue();
				EmbedBuilder builder = new EmbedBuilder();
				var list = queue.getOrDefault(event.getGuild(), new ArrayList<>());
				if (list.isEmpty()) {
					builder.setTitle("현재 재생 중인 음악이 없습니다");
					builder.setDescription("현재 반복 상태 : " + (repeat.contains(event.getGuild()) ? "활성화" : "비활성화"));
				} else {
					AudioTrackInfo np = list.get(0).getInfo();
					builder.setTitle("현재 재생 중 : " + np.title + "\n(" + URLUtils.getVideoIdNoThrow(np.uri) + ")");
					long time = 0;
					for (AudioTrack audioTrack : list) {
						time += audioTrack.getDuration();
					}
					StringBuilder desc = new StringBuilder("총 재생 시간 : %1$s\n".formatted(StringUtils.parseUnixTime(time, false)));
					desc.append("현재 반복 상태 : ").append(repeat.contains(event.getGuild()) ? "활성화" : "비활성화").append("\n");
					if (list.size() > 1) {
						for (int i = 1; i < list.size(); i++) {
							AudioTrackInfo info = list.get(i).getInfo();
							desc.append(i).append(".").append(info.title).append(" (").append(URLUtils.getVideoIdNoThrow(info.uri)).append(")");
							if (i + 1 != list.size()) {
								desc.append("\n");
							}
						}
					}
					builder.setDescription(desc.toString());
				}
				message.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
			}
			if (message.getContentRaw().equals(commandPrefix + "np") || message.getContentRaw().equals(commandPrefix + "nowplaying")) {
				message.addReaction(Emoji.fromUnicode("✅")).queue();
				EmbedBuilder builder = new EmbedBuilder();
				var list = queue.getOrDefault(event.getGuild(), new ArrayList<>());
				if (list.isEmpty()) {
					builder.setTitle("현재 재생 중인 음악이 없습니다");
				} else {
					AudioTrack nt = list.get(0);
					AudioTrackInfo np = nt.getInfo();
					builder.setTitle("현재 재생 중 : " + np.title + "\n(" + URLUtils.getVideoIdNoThrow(np.uri) + ")");
					String index2 = StringUtils.parseUnixTime(nt.getDuration(), false);
					String index1 = StringUtils.parseUnixTime(nt.getPosition(), index2.split(":").length >= 3);
					builder.setDescription(index1 + " / " + index2);
				}
				message.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
			}
			if (message.getContentRaw().equals(commandPrefix + "shutdown") || message.getContentRaw().equals(commandPrefix + "셧다운")) {
				if (event.getMember().isOwner()) {
					message.addReaction(Emoji.fromUnicode("✅")).queue();
					e.getJDA().shutdown();
				}
			}
		}
	}
}
