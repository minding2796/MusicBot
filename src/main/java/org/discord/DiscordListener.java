package org.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.AudioManager;
import org.discord.utils.*;
import org.discord.utils.korean.KoreanUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.min;
import static org.discord.MusicBot.*;

public class DiscordListener implements EventListener {
	@Override
	public void onEvent(@NotNull GenericEvent e) {
		if (e instanceof MessageReceivedEvent event) {
			User u = event.getAuthor();
			if (u.isBot()) return;
			if (u.isSystem()) return;
			Message message = event.getMessage();
			commandExecute(message, u, event);
		}
		if (e instanceof GuildVoiceUpdateEvent event) {
			if (event.getChannelLeft() == null) return;
			if (event.getChannelLeft().getMembers().isEmpty()) return;
			var list = new ArrayList<>(event.getChannelLeft().getMembers());
			list.removeIf(m -> m.getUser().isBot());
			list.removeIf(m -> m.getUser().isSystem());
			if (list.isEmpty()) {
				AudioManager audioManager = event.getGuild().getAudioManager();
				if (!event.getChannelLeft().equals(audioManager.getConnectedChannel())) return;
				if (queue.get(event.getGuild()) != null) queue.get(event.getGuild()).get(0).b().reply("음성 채널에 남아있는 사람이 없어 재생을 중지하고 대기열을 초기화했어요!").mentionRepliedUser(false).queue();
				queue.remove(event.getGuild());
				AudioPlayer p = null;
				if (event.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
					p = hd.audioPlayer;
				}
				if (p != null) {
					p.stopTrack();
				}
				audioManager.closeAudioConnection();
			}
		}
	}
	
	
	public void playExecute(Message message, User u, MessageReceivedEvent e, @NotNull Message originalmessage, boolean isSoundcloud) {
		if (e.getMember().getVoiceState().inAudioChannel()) {
			int len = 0;
			if (message.getContentRaw().startsWith(commandPrefix + "p ")) len = commandPrefix.length() + 2;
			if (message.getContentRaw().startsWith(commandPrefix + "play ")) len = commandPrefix.length() + 5;
			if (message.getContentRaw().startsWith(commandPrefix + "sp ")) len = commandPrefix.length() + 3;
			if (message.getContentRaw().startsWith(commandPrefix + "soundcloudplay ")) len = commandPrefix.length() + 15;
			AudioTrack item;
			Iterator<AudioTrack> iterator = null;
			boolean allowlist = false;
			if (isSoundcloud) {
				String url = message.getContentRaw().substring(len);
				message.addReaction(Emoji.fromUnicode("✅")).queue();
				var playerManager = new DefaultAudioPlayerManager();
				AudioSources.registerSources(playerManager);
				item = SoundCloudAudioSourceManager.createDefault().loadFromTrackPage(url);
			} else {
				Tuple<String, String> videoid;
					try {
						videoid = URLUtils.getVideoId(message.getContentRaw().substring(len));
					} catch (MalformedURLException | UnsupportedOperationException | IllegalArgumentException exception) {
						videoid = new Tuple<>(message.getContentRaw().substring(len), null);
					}
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				String listid = videoid.b() != null ? videoid.b() : null;
				var youtubeAudioSourceManager = new YTSourceManager(true);
				youtubeAudioSourceManager.setPlaylistPageCount(100);
				AudioItem audioItem;
				if (listid != null) {
					audioItem = youtubeAudioSourceManager.playlist(listid, videoid.a());
					allowlist = true;
				} else {
					if (URLUtils.isVideoId(videoid.a())) {
						audioItem = youtubeAudioSourceManager.loadTrackWithVideoId(videoid.a(), true);
					} else {
						audioItem = youtubeAudioSourceManager.search(videoid.a());
					}
				}
				if (audioItem instanceof AudioTrack t) {
					item = t;
				} else if (audioItem instanceof AudioPlaylist p) {
					if (allowlist) {
						item = null;
						var list = p.getTracks();
						iterator = list.iterator();
					} else {
						item = p.getTracks().get(0);
					}
				} else {
					item = null;
				}
			}
			if (allowlist && iterator != null) {
				while (iterator.hasNext()) {
					item = iterator.next();
					if (queue.getOrDefault(e.getGuild(), new ArrayList<>()).isEmpty()) {
						var playerManager = new DefaultAudioPlayerManager();
						AudioSources.registerSources(playerManager);
						var player = playerManager.createPlayer();
						player.addListener(evt -> {
							if (evt instanceof TrackExceptionEvent evnt) {
								AtomicReference<AudioTrack> track = new AtomicReference<>(null);
								new Thread(() -> {
									while (track.get() == null) {
										try {
											var youtubeAudioSourceManager = new YTSourceManager(true);
											youtubeAudioSourceManager.setPlaylistPageCount(100);
											track.set((AudioTrack) youtubeAudioSourceManager.loadTrackWithVideoId(evnt.track.getInfo().uri, true));
										} catch (FriendlyException ignored) {
										}
									}
									evnt.player.playTrack(track.get());
									Thread.interrupted();
								}).start();
								return;
							}
							if (evt instanceof TrackEndEvent evnt) {
								Guild guild = e.getGuild();
								var list = queue.get(guild);
								if (evnt.endReason.equals(AudioTrackEndReason.LOAD_FAILED)) {
									AtomicReference<AudioTrack> track = new AtomicReference<>(null);
									new Thread(() -> {
										while (track.get() == null) {
											try {
												var youtubeAudioSourceManager = new YTSourceManager(true);
												youtubeAudioSourceManager.setPlaylistPageCount(100);
												track.set((AudioTrack) youtubeAudioSourceManager.loadTrackWithVideoId(evnt.track.getInfo().uri, true));
											} catch (FriendlyException ignored) {
											}
										}
										evnt.player.playTrack(track.get());
										Thread.interrupted();
									}).start();
									return;
								}
								if (repeat.contains(guild)) {
									AudioTrack track = list.get(0).a().makeClone();
									track.setPosition(0L);
									list.add(new Tuple<>(track, list.get(0).b()));
								}
								list.remove(0);
								queue.remove(guild);
								queue.put(guild, list);
								if (!list.isEmpty()) {
									evt.player.playTrack(list.get(0).a());
								} else {
									guild.getAudioManager().closeAudioConnection();
								}
							}
						});
						var list = new ArrayList<Tuple<AudioTrack, Message>>();
						list.add(new Tuple<>(item, originalmessage));
						queue.put(e.getGuild(), list);
						if (volume.containsKey(e.getGuild())) {
							player.setVolume(volume.get(e.getGuild()));
						}
						player.startTrack(item, false);
						var handler = new AudioPlayerSendHandler(player);
						var voiceChannel = e.getMember().getVoiceState().getChannel();
						var audioManager = e.getGuild().getAudioManager();
						audioManager.setSelfDeafened(true);
						audioManager.setSendingHandler(handler);
						audioManager.openAudioConnection(voiceChannel);
					} else {
						var list = queue.get(e.getGuild());
						list.add(new Tuple<>(item, originalmessage));
						queue.remove(e.getGuild());
						queue.put(e.getGuild(), list);
					}
				}
			} else {
				if (queue.getOrDefault(e.getGuild(), new ArrayList<>()).isEmpty()) {
					var playerManager = new DefaultAudioPlayerManager();
					AudioSources.registerSources(playerManager);
					var player = playerManager.createPlayer();
					player.addListener((evt) -> {
						if (evt instanceof TrackExceptionEvent evnt) {
							AtomicReference<AudioTrack> track = new AtomicReference<>(null);
							new Thread(() -> {
								while (track.get() == null) {
									try {
										var youtubeAudioSourceManager = new YTSourceManager(true);
										youtubeAudioSourceManager.setPlaylistPageCount(100);
										track.set((AudioTrack) youtubeAudioSourceManager.loadTrackWithVideoId(evnt.track.getInfo().uri, true));
									} catch (FriendlyException ignored) {
									}
								}
								evnt.player.playTrack(track.get());
								Thread.interrupted();
							}).start();
							return;
						}
						if (evt instanceof TrackEndEvent evnt) {
							Guild guild = e.getGuild();
							var list = queue.getOrDefault(guild, new ArrayList<>());
							if (evnt.endReason.equals(AudioTrackEndReason.LOAD_FAILED)) {
								AtomicReference<AudioTrack> track = new AtomicReference<>(null);
								new Thread(() -> {
									while (track.get() == null) {
										try {
											if (isSoundcloud) {
												int l = 0;
												if (message.getContentRaw().startsWith(commandPrefix + "sp ")) l = commandPrefix.length() + 3;
												if (message.getContentRaw().startsWith(commandPrefix + "soundcloudplay ")) l = commandPrefix.length() + 15;
												track.set(SoundCloudAudioSourceManager.createDefault().loadFromTrackPage(message.getContentRaw().substring(l)));
											} else {
												var youtubeAudioSourceManager = new YTSourceManager(true);
												youtubeAudioSourceManager.setPlaylistPageCount(100);
												track.set((AudioTrack) youtubeAudioSourceManager.loadTrackWithVideoId(evnt.track.getInfo().uri, true));
											}
										} catch (FriendlyException ignored) {
										}
									}
									evnt.player.playTrack(track.get());
									Thread.interrupted();
								}).start();
								return;
							}
							if (repeat.contains(guild)) {
								AudioTrack track = list.get(0).a().makeClone();
								track.setPosition(0L);
								list.add(new Tuple<>(track, list.get(0).b()));
							}
							list.remove(0);
							queue.remove(guild);
							queue.put(guild, list);
							if (!list.isEmpty()) {
								evt.player.playTrack(list.get(0).a());
							} else {
								guild.getAudioManager().closeAudioConnection();
							}
						}
					});
					var list = new ArrayList<Tuple<AudioTrack, Message>>();
					list.add(new Tuple<>(item, originalmessage));
					queue.put(e.getGuild(), list);
					if (volume.containsKey(e.getGuild())) {
						player.setVolume(volume.get(e.getGuild()));
					}
					player.startTrack(item, false);
					var handler = new AudioPlayerSendHandler(player);
					var voiceChannel = e.getMember().getVoiceState().getChannel();
					var audioManager = e.getGuild().getAudioManager();
					audioManager.setSelfDeafened(true);
					audioManager.setSendingHandler(handler);
					audioManager.openAudioConnection(voiceChannel);
				} else {
					var list = queue.get(e.getGuild());
					list.add(new Tuple<>(item, originalmessage));
					queue.remove(e.getGuild());
					queue.put(e.getGuild(), list);
				}
			}
		} else {
			originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
			originalmessage.reply("오디오 채널에 들어가야 해당 명령어를 사용할 수 있어요!").mentionRepliedUser(false).queue();
		}
	}
	
	private void commandExecute(Message message, User u, MessageReceivedEvent e) {
		commandExecute(message, u, e, message);
	}
	
	public void commandExecute(Message message, User u, MessageReceivedEvent e, @NotNull Message originalmessage) {
		if (message.getContentRaw().equals(commandPrefix + "songrepeat") || message.getContentRaw().equals(commandPrefix + "sr")) {
			originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
			if (repeat.contains(e.getGuild())) {
				repeat.remove(e.getGuild());
			} else {
				repeatSingle.remove(e.getGuild());
				repeat.add(e.getGuild());
			}
			return;
		}
		if (message.getContentRaw().startsWith(commandPrefix + "songrepeat ") || message.getContentRaw().startsWith(commandPrefix + "sr ")) {
			int l = 0;
			if (message.getContentRaw().startsWith(commandPrefix + "songrepeat ")) l = commandPrefix.length() + 11;
			if (message.getContentRaw().startsWith(commandPrefix + "sr ")) l = commandPrefix.length() + 3;
			String arg = message.getContentRaw().substring(l);
			if (arg.equalsIgnoreCase("single")) {
				if (message.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
					if (repeatSingle.contains(e.getGuild())) {
						repeatSingle.remove(e.getGuild());
					} else {
						repeat.remove(e.getGuild());
						repeatSingle.add(e.getGuild());
					}
				} else {
					originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
					originalmessage.reply("곡을 한곡만 반복할 권한이 없어요!").mentionRepliedUser(false).queue();
				}
			} else if (arg.equalsIgnoreCase("off")) {
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				repeat.remove(e.getGuild());
				repeatSingle.remove(e.getGuild());
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				if (repeat.contains(e.getGuild())) {
					repeat.remove(e.getGuild());
				} else {
					repeatSingle.remove(e.getGuild());
					repeat.add(e.getGuild());
				}
			}
			return;
		}
		if (message.getContentRaw().equals(commandPrefix + "commands")) {
			originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle(":scroll: command list");
			builder.setDescription("""
					----- 기본 기능 -----
					%1$scommands : 이 항목을 표시합니다
					%1$sshutdown | %1$s셧다운 : 봇을 종료시킵니다 (서버 주인 전용)
					----- 뮤직봇 -----
					%1$sp | %1$splay : 음악을 재생합니다
					%1$ssr | %1$ssongrepeat : 음악 재생을 반복합니다
					%1$sq | %1$squeue : 현재 대기열을 표시합니다
					%1$snp | %1$snowplaying : 현재 재생중인 음악을 표시합니다
					%1$ssi | %1$ssonginfo: 음악의 정보를 표시합니다
					%1$sps | %1$spause : 재생중인 음악을 일시정지합니다
					%1$svol | %1$svolume : 재생중인 음악의 볼륨을 조절합니다
					%1$spf | %1$splayfirst : 음악을 다음 순서에 바로 재생합니다 (관리자 전용)
					%1$st | %1$stime : 재생중인 음악의 시간을 조정합니다 (관리자 전용)
					%1$sskip | %1$ssk : 재생중인 음악을 건너뜁니다 (관리자 전용)
					%1$sstop | %1$sst : 현재 재생중인 음악을 강제로 중지합니다 (관리자 전용)
					%1$sremove : 노래를 대기열에서 제거합니다 (관리자 전용)
					%1$sclear : 대기열을 삭제합니다 (관리자 전용)
					""".formatted(commandPrefix));
			builder.setColor(Color.GREEN);
			originalmessage.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
			return;
		}
		if (message.getContentRaw().startsWith(commandPrefix + "p ") || message.getContentRaw().startsWith(commandPrefix + "play ")) {
			playExecute(message, u, e, originalmessage, false);
		}
		if (message.getContentRaw().equals(commandPrefix + "p") || message.getContentRaw().equals(commandPrefix + "play")) {
			playExecute(message, u, e, originalmessage, false);
		}
		if (message.getContentRaw().startsWith(commandPrefix + "pf ") || message.getContentRaw().startsWith(commandPrefix + "playfirst ")) {
			if (originalmessage.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				playExecute(message, u, e, originalmessage, false);
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("대기열을 무시하고 재생할 권한이 없어요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().startsWith(commandPrefix + "volume ") || message.getContentRaw().startsWith(commandPrefix + "vol ")) {
			int len = 0;
			if (message.getContentRaw().startsWith(commandPrefix + "vol ")) len = commandPrefix.length() + 4;
			if (message.getContentRaw().startsWith(commandPrefix + "volume ")) len = commandPrefix.length() + 7;
			String vol = message.getContentRaw().substring(len);
			AudioPlayer p = null;
			if (e.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
				p = hd.audioPlayer;
			}
			if (p != null) {
				try {
					if (!e.getGuild().getAudioManager().getConnectedChannel().equals(originalmessage.getMember().getVoiceState().getChannel())) {
						originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
						originalmessage.reply("음성 채널에 들어와야 사용할 수 있어요!").mentionRepliedUser(false).queue();
						return;
					}
					int voli = Integer.parseInt(vol);
					if (voli < 0 || voli > 100) {
						originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
						originalmessage.reply("숫자 범위는 0~100까지에요!").mentionRepliedUser(false).queue();
						return;
					}
					p.setVolume(voli);
					volume.remove(e.getGuild());
					volume.put(e.getGuild(), voli);
					originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				} catch (NumberFormatException exception) {
					originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
					originalmessage.reply("숫자만 입력 가능해요!").mentionRepliedUser(false).queue();
				} catch (Throwable t) {
					originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
					originalmessage.reply("음성 채널에 들어와야 사용할 수 있어요!").mentionRepliedUser(false).queue();
					return;
				}
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("현재 재생중인 음악이 없어요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().startsWith(commandPrefix + "time ") || message.getContentRaw().startsWith(commandPrefix + "t ")) {
			if (e.getMember().hasPermission(Permission.ADMINISTRATOR) || queue.get(e.getGuild()).get(0).b().getAuthor().equals(u)) {
				int len = 0;
				if (message.getContentRaw().startsWith(commandPrefix + "t ")) len = commandPrefix.length() + 2;
				if (message.getContentRaw().startsWith(commandPrefix + "time ")) len = commandPrefix.length() + 5;
				String time = message.getContentRaw().substring(len);
				AudioPlayer p = null;
				if (e.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
					p = hd.audioPlayer;
				}
				if (p != null) {
					try {
						long unixtime = StringUtils.fromParsedUnixtime(time);
						p.getPlayingTrack().setPosition(unixtime);
						originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
					} catch (IllegalArgumentException exception) {
						originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
						originalmessage.reply("hh:mm:ss 형식으로 입력 가능해요!").mentionRepliedUser(false).queue();
					}
				} else {
					originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
					originalmessage.reply("현재 재생중인 음악이 없어요!").mentionRepliedUser(false).queue();
				}
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("음악 재생 시간을 변경할 권한이 없는것 같아요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().equals(commandPrefix + "volume") || message.getContentRaw().equals(commandPrefix + "vol")) {
			AudioPlayer p = null;
			if (e.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
				p = hd.audioPlayer;
			}
			if (p != null) {
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				int vol = p.getVolume();
				originalmessage.reply("현재 볼륨은 " + vol + KoreanUtils.getPostposition(KoreanUtils.readToKorean((long) vol), "이", "", false) + "에요!").mentionRepliedUser(false).queue();
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("현재 재생중인 음악이 없어요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().equals(commandPrefix + "pause") || message.getContentRaw().equals(commandPrefix + "ps")) {
			AudioPlayer p = null;
			if (e.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
				p = hd.audioPlayer;
			}
			if (p != null) {
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				p.setPaused(!p.isPaused());
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("현재 재생중인 음악이 없어요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().equals(commandPrefix + "sk") || message.getContentRaw().equals(commandPrefix + "skip")) {
			AudioPlayer p = null;
			if (e.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
				p = hd.audioPlayer;
			}
			if (p != null) {
				if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
					p.stopTrack();
				} else {
					if (e.getMember().equals(queue.get(e.getGuild()).get(0).b().getMember())) {
						originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
						p.stopTrack();
					} else {
						originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
						originalmessage.reply("음악 재생을 강제종료할 권한이 없는것 같아요!").mentionRepliedUser(false).queue();
					}
				}
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("현재 재생중인 음악이 없어요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().startsWith(commandPrefix + "remove ")) {
			AtomicBoolean isInterrupted = new AtomicBoolean(false);
			Thread thread = new Thread(() -> {
				List<Integer> removed = new ArrayList<>();
				int errorstatus = 0;
				List<Integer> list = StringUtils.parseRange(message.getContentRaw().substring(commandPrefix.length() + 7));
				list.sort(Comparator.reverseOrder());
				for (Integer index : list) {
					try {
						if (index == 0) {
							AudioPlayer p = null;
							if (e.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
								p = hd.audioPlayer;
							}
							if (p != null) {
								if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
									removed.add(index);
									p.stopTrack();
								} else {
									if (e.getMember().equals(queue.get(e.getGuild()).get(0).b().getMember())) {
										removed.add(index);
										p.stopTrack();
									} else {
										errorstatus = 1;
									}
								}
							} else {
								errorstatus = 2;
							}
						} else {
							var audiotrack = queue.get(e.getGuild());
							if (audiotrack != null) {
								try {
									if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
										audiotrack.remove((int) index);
										queue.remove(e.getGuild());
										queue.put(e.getGuild(), audiotrack);
										removed.add(index);
									} else {
										if (e.getMember().equals(audiotrack.get(index).b().getMember())) {
											audiotrack.remove((int) index);
											queue.remove(e.getGuild());
											queue.put(e.getGuild(), audiotrack);
											removed.add(index);
										} else {
											errorstatus = 1;
										}
									}
								} catch (IndexOutOfBoundsException exception) {
									errorstatus = 3;
								}
							} else {
								errorstatus = 4;
							}
						}
					} catch (IllegalArgumentException exception) {
						errorstatus = 5;
					}
				}
				if (errorstatus != 0 && removed.isEmpty()) {
					originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
					if (errorstatus == 1) {
						originalmessage.reply("음악을 제거할 권한이 없는것 같아요!").mentionRepliedUser(false).queue();
					} else if (errorstatus == 2) {
						originalmessage.reply("현재 재생중인 음악이 없어요!").mentionRepliedUser(false).queue();
					} else if (errorstatus == 3) {
						originalmessage.reply("숫자를 잘 골라주세요! 대기열에 없는거 말고요!").mentionRepliedUser(false).queue();
					} else if (errorstatus == 4) {
						originalmessage.reply("현재 재생중인 대기열이 없어요!").mentionRepliedUser(false).queue();
					} else {
						originalmessage.reply("숫자 범위에 맞는 형식만 입력해주세요!").mentionRepliedUser(false).queue();
					}
				} else {
					removed.sort(Comparator.naturalOrder());
					originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
					originalmessage.reply(removed.toString().substring(1, removed.toString().length() - 1) + "에 있는 노래를 성공적으로 제거했어요!").mentionRepliedUser(false).queue();
				}
				isInterrupted.set(true);
				Thread.currentThread().interrupt();
			});
			thread.start();
			new Thread(() -> {
				try {
					Thread.sleep(1000L);
					if (!isInterrupted.get()) {
						thread.interrupt();
						originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
						originalmessage.reply("숫자 범위가 너무 커서 범위를 해석하지 못했어요!").mentionRepliedUser(false).queue();
					}
					Thread.currentThread().interrupt();
				} catch (InterruptedException ignored) {
				}
			}).start();
		}
		if (message.getContentRaw().equals(commandPrefix + "clear")) {
			if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				try {
					var audiotrack = queue.get(e.getGuild());
					if (audiotrack != null) {
						var temp = audiotrack.get(0);
						audiotrack.clear();
						audiotrack.add(temp);
						queue.remove(e.getGuild());
						queue.put(e.getGuild(), audiotrack);
						originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
					} else {
						originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
						originalmessage.reply("현재 재생중인 대기열이 없어요!").mentionRepliedUser(false).queue();
					}
				} catch (NumberFormatException exception) {
					originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
					originalmessage.reply("숫자만 입력해주세요!").mentionRepliedUser(false).queue();
				}
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("음악 재생을 강제종료할 권한이 없는것 같아요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().equals(commandPrefix + "st") || message.getContentRaw().equals(commandPrefix + "stop")) {
			if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				AudioPlayer p = null;
				queue.remove(e.getGuild());
				if (e.getGuild().getAudioManager().getSendingHandler() instanceof AudioPlayerSendHandler hd) {
					p = hd.audioPlayer;
				}
				if (p != null) {
					p.stopTrack();
				}
				AudioManager audioManager = e.getGuild().getAudioManager();
				audioManager.closeAudioConnection();
			} else {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("음악 재생을 강제종료할 권한이 없는것 같아요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().startsWith(commandPrefix + "q ") || message.getContentRaw().startsWith(commandPrefix + "queue ")) {
			try {
				int len = 0;
				if (message.getContentRaw().startsWith(commandPrefix + "q ")) len = commandPrefix.length() + 2;
				if (message.getContentRaw().startsWith(commandPrefix + "queue ")) len = commandPrefix.length() + 6;
				int index = Integer.parseInt(message.getContentRaw().substring(len)) - 1;
				if (index < 0) {
					throw new IllegalArgumentException("negative exception");
				}
				EmbedBuilder builder = new EmbedBuilder();
				var list = queue.getOrDefault(e.getGuild(), new ArrayList<>());
				if (index + 1 > Math.max((list.size() - 1) / 10 + 1 - (((list.size() - 1) % 10) == 0 ? 1 : 0), 1)) {
					throw new IndexOutOfBoundsException("index out of bound");
				}
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				if (list.isEmpty()) {
					builder.setTitle("현재 재생 중인 음악이 없어요!");
					builder.setDescription("현재 반복 상태 : " + (repeatSingle.contains(e.getGuild()) ? "한곡만" : repeat.contains(e.getGuild()) ? "활성화" : "비활성화"));
				} else {
					AudioTrackInfo np = list.get(0).a().getInfo();
					builder.setTitle("현재 재생 중 : " + np.title, np.uri);
					long time = 0;
					for (Tuple<AudioTrack, Message> audioTrack : list) {
						time += audioTrack.a().getDuration();
					}
					StringBuilder desc = new StringBuilder("총 재생 시간 : %1$s / %2$s\n".formatted(StringUtils.parseUnixTime(list.get(0).a().getPosition(), StringUtils.parseUnixTime(time, false).split(":").length >= 3), StringUtils.parseUnixTime(time, false)));
					desc.append("현재 반복 상태 : ").append(repeatSingle.contains(e.getGuild()) ? "한곡만" : repeat.contains(e.getGuild()) ? "활성화" : "비활성화").append("\n");
					if (list.size() > (index * 10) + 1) {
						for (int i = (index * 10) + 1; i < min(list.size(), (index * 10) + 11); i++) {
							AudioTrackInfo info = list.get(i).a().getInfo();
							desc.append("[").append(i).append(".").append(info.title).append("](").append(info.uri).append(")");
							desc.append("\n");
						}
					}
					desc.append("현재 페이지 : ").append(index + 1).append(" / ").append(Math.max((list.size() - 1) / 10 + 1 - (((list.size() - 1) % 10) == 0 ? 1 : 0), 1));
					builder.setDescription(desc.toString());
				}
				originalmessage.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
			} catch (NumberFormatException exception) {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("숫자만 입력해주세요!").mentionRepliedUser(false).queue();
			} catch (IllegalArgumentException exception) {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("자연수만 입력할 수 있어요!").mentionRepliedUser(false).queue();
			} catch (IndexOutOfBoundsException exception) {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("대기열 페이지 내의 범위에서만 입력해주세요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().startsWith(commandPrefix + "si ") || message.getContentRaw().startsWith(commandPrefix + "songinfo ")) {
			try {
				int len = 0;
				if (message.getContentRaw().startsWith(commandPrefix + "si ")) len = commandPrefix.length() + 3;
				if (message.getContentRaw().startsWith(commandPrefix + "songinfo ")) len = commandPrefix.length() + 9;
				int index = Integer.parseInt(message.getContentRaw().substring(len));
				if (index < 0) {
					throw new IllegalArgumentException("negative exception");
				}
				EmbedBuilder builder = new EmbedBuilder();
				var list = queue.getOrDefault(e.getGuild(), new ArrayList<>());
				originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
				if (list.isEmpty()) {
					builder.setTitle("현재 재생 중인 음악이 없어요!");
				} else {
					AudioTrack track = list.get(index).a();
					builder.setTitle(track.getInfo().title, track.getInfo().uri);
					builder.setDescription("곡 링크 : " + track.getInfo().uri + "\n신청자 : " + list.get(index).b().getAuthor().getName() + "\n곡 길이 : " + StringUtils.parseUnixTime(track.getInfo().length, false));
				}
				originalmessage.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
			} catch (NumberFormatException exception) {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("숫자만 입력해주세요!").mentionRepliedUser(false).queue();
			} catch (IllegalArgumentException exception) {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("0과 자연수만 입력할 수 있어요!").mentionRepliedUser(false).queue();
			} catch (IndexOutOfBoundsException exception) {
				originalmessage.addReaction(Emoji.fromUnicode("⛔")).queue();
				originalmessage.reply("대기열 신청곡 내의 범위에서만 입력해주세요!").mentionRepliedUser(false).queue();
			}
		}
		if (message.getContentRaw().equals(commandPrefix + "si") || message.getContentRaw().equals(commandPrefix + "songinfo")) {
			EmbedBuilder builder = new EmbedBuilder();
			var list = queue.getOrDefault(e.getGuild(), new ArrayList<>());
			originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
			if (list.isEmpty()) {
				builder.setTitle("현재 재생 중인 음악이 없어요!");
			} else {
				AudioTrack track = list.get(0).a();
				builder.setTitle(track.getInfo().title, track.getInfo().uri);
				builder.setDescription("곡 링크 : " + track.getInfo().uri + "\n신청자 : " + list.get(0).b().getAuthor().getName() + "\n곡 길이 : " + StringUtils.parseUnixTime(track.getInfo().length, false));
			}
			originalmessage.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
		}
		if (message.getContentRaw().equals(commandPrefix + "q") || message.getContentRaw().equals(commandPrefix + "queue")) {
			originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
			EmbedBuilder builder = new EmbedBuilder();
			var list = queue.getOrDefault(e.getGuild(), new ArrayList<>());
			if (list.isEmpty()) {
				builder.setTitle("현재 재생 중인 음악이 없어요!");
				builder.setDescription("현재 반복 상태 : " + (repeatSingle.contains(e.getGuild()) ? "한곡만" : repeat.contains(e.getGuild()) ? "활성화" : "비활성화"));
			} else {
				if (list.get(0).a() == null) return;
				AudioTrackInfo np = list.get(0).a().getInfo();
				builder.setTitle("현재 재생 중 : " + np.title, np.uri);
				long time = 0;
				for (Tuple<AudioTrack, Message> audioTrack : list) {
					time += audioTrack.a().getDuration();
				}
				StringBuilder desc = new StringBuilder("총 재생 시간 : %1$s / %2$s\n".formatted(StringUtils.parseUnixTime(list.get(0).a().getPosition(), StringUtils.parseUnixTime(time, false).split(":").length >= 3), StringUtils.parseUnixTime(time, false)));
				desc.append("현재 반복 상태 : ").append(repeatSingle.contains(e.getGuild()) ? "한곡만" : repeat.contains(e.getGuild()) ? "활성화" : "비활성화").append("\n");
				if (list.size() > 1) {
					for (int i = 1; i < min(list.size(), 11); i++) {
						AudioTrackInfo info = list.get(i).a().getInfo();
						desc.append("[").append(i).append(".").append(info.title).append("](").append(info.uri).append(")");
						desc.append("\n");
					}
				}
				desc.append("현재 페이지 : 1 / ").append(Math.max((list.size() - 1) / 10 + 1 - (((list.size() - 1) % 10) == 0 ? 1 : 0), 1));
				builder.setDescription(desc.toString());
			}
			originalmessage.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
		}
		if (message.getContentRaw().equals(commandPrefix + "np") || message.getContentRaw().equals(commandPrefix + "nowplaying")) {
			originalmessage.addReaction(Emoji.fromUnicode("✅")).queue();
			EmbedBuilder builder = new EmbedBuilder();
			var list = queue.getOrDefault(e.getGuild(), new ArrayList<>());
			if (list.isEmpty()) {
				builder.setTitle("현재 재생 중인 음악이 없어요!");
			} else {
				AudioTrack nt = list.get(0).a();
				AudioTrackInfo np = nt.getInfo();
				builder.setTitle("현재 재생 중 : " + np.title, np.uri);
				String index2 = StringUtils.parseUnixTime(nt.getDuration(), false);
				String index1 = StringUtils.parseUnixTime(nt.getPosition(), index2.split(":").length >= 3);
				builder.setDescription(index1 + " / " + index2);
			}
			originalmessage.replyEmbeds(builder.build()).mentionRepliedUser(false).queue();
		}
		if (message.getContentRaw().equals(commandPrefix + "shutdown") || message.getContentRaw().equals(commandPrefix + "셧다운")) {
			if (e.getMember().isOwner()) {
				message.addReaction(Emoji.fromUnicode("✅")).queue();
				e.getJDA().shutdown();
			}
		}
	}
}
