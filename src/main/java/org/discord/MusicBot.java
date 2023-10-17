package org.discord;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.discord.utils.FileUtils;
import org.discord.utils.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class MusicBot {
	public static JDA jda;
	static String commandPrefix = "!";
	static HashMap<Guild, List<Tuple<AudioTrack, Message>>> queue = new HashMap<>();
	static HashMap<Guild, Integer> volume = new HashMap<>();
	static List<Guild> repeat = new ArrayList<>();
	static List<Guild> repeatSingle = new ArrayList<>();
	public static void main(String[] args) {
		try {
			JDABuilder builder = JDABuilder.createDefault(FileUtils.readFile(new File("BOTTOKEN.token")));
			builder.setAutoReconnect(true);
			builder.setMemberCachePolicy(MemberCachePolicy.ALL);
			builder.setStatus(OnlineStatus.ONLINE);
			builder.setActivity(Activity.playing("%1$scommands".formatted(commandPrefix)));
			builder.addEventListeners(new DiscordListener());
			builder.enableIntents(
					GatewayIntent.MESSAGE_CONTENT,
					GatewayIntent.GUILD_MEMBERS,
					GatewayIntent.GUILD_PRESENCES
			);
			jda = builder.build();
		} catch (Throwable e) {
			try {
				File file = new File("BOTTOKEN.token");
				if (!file.exists()) file.createNewFile();
			} catch (IOException ignored) {
			}
			e.printStackTrace(System.out);
			System.err.print("봇토큰을 찾을 수 없습니다\nBOTTOKEN.token을 확인해 주세요\n(엔터시 종료)");
			new Scanner(System.in).nextLine();
			System.exit(0);
		}
	}
}
