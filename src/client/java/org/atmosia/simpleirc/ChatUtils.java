package org.atmosia.simpleirc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.atmosia.simpleirc.classes.MessageHandler;

import java.util.Map;

public enum ChatUtils{
	; // <-- WHAT THE FUCK?????
    // I just tried to remove it and got 34 compilation errors.
    // I cannot describe my fucking confusion - FJSAGS
	
	private static final MinecraftClient MCInstance = MinecraftClient.getInstance();

	public static void component(Text component)
	{
		ChatHud chatHud = MCInstance.inGameHud.getChatHud();
		chatHud.addMessage(component);
	}
	
	public static void message(String message)
	{
		component (Text.literal(message));
	}
	
	public static void sudomessage(String message)
	{
		//MC.player.sendChatMessage(message, null);
		MCInstance.getNetworkHandler().sendChatMessage(message);
		
	}
	
	public static String getUsername()
	{
		return MCInstance.player.getName().getString();
	}

    public static void Error(Exception e) {
        var message = e.getMessage().replace("§r", "§c"); // to allow custom formatting mid-sentence;
        message("§c[ERR] " + message);
    }
    public static void Error(String message) {
        message = message.replace("§r", "§c"); // to allow custom formatting mid-sentence;
        message("§c[ERR] " + message);
    }
    public static void Info(String message) {
        message = message.replace("§r", "§7");
        message("§7[INFO] " + message);
    }
    public static void Warn(String message) {
        message = message.replace("§r", "§e");
        message("§e" + message);
    }
    public static void RawOut(String message) {
        if (MainClient.irc.Verbosity != IrcVerbosity.RAW) return;
        message = message.replace("§r", "§e");
        message("§e[RAW =>] " + message);
    }
    public static void RawIn(String message) {
        if (MainClient.irc.Verbosity != IrcVerbosity.RAW) return;
        message = message.replace("§r", "§8");
        message("§7[RAW <=] " + message);
    }
    public static void Success(String message) {
        message = message.replace("§r", "§a");
        message("§a[SUCCESS] " + message);
    }
    public static void Notify(String message) {
        message = message.replace("§r", "§9");
        message("§a[INFO] §9" + message);

    }
    public static void Verbose(String message) {
        if (MainClient.irc.Verbosity != IrcVerbosity.VERBOSE &&
        MainClient.irc.Verbosity != IrcVerbosity.RAW) return;
        message = message.replace("§r", "§9");

        message("§9[VERB] " + message);
    }
    public static class IRCMessageTemplates {
        public static void Notice(String source, String channel, String content) {
            // Target:
            // &9[Channel]&r | &6[NOTICE]&r <&[Source]&r> [Contents]
            String message = "§9" + channel + "§r | §6[NOTICE]§r <§c" + source + "§r> " + content;
            message(message);
        }
        public static void Message(String source, String channel, String content) {
            // #ss15 | <FJSAGS_Web> i need this for testing and understanding of IRC protocol...
            // §9#ss15§r | <§cFJSAGS_Web§r> i need this for testing and understanding of IRC protocol...
            if (channel.startsWith("#")) {
                String message = "§9" + channel + "§r | <§c" + FormatRoles(channel, source).replace("§r", "§c") + "§r> "
                        + content;
                message(message);
            }
            else {
                String message = "§6" + channel + " [PM]§r | <§c" + source + "§r> " + content;
                message(message);
            }
        }
        public static void Topic(String channel, String content) {
            String message = "§9" + channel + "§r | §6Topic§r: " + content;
            message(message);
        }
        public static void TopicSetBy(String channel, String content) {
            String message = "§9 " + channel + "§r | §6Topic set by§r: " + MessageHandler.GetFormattedUser(content, channel);
            message(message);
        }
        public static void Join(String source, String channel) {
            String message = "§9" + channel + "§r | + " + FormatRoles(channel, source);
            message(message);
        }
        public static void Part(String source, String channel, String reason) {
            String message = "§9" + channel + "§r | - " + FormatRoles(channel, source) + ": " + reason;
            message(message);
        }

    }
    private static String FormatRoles(String channel, String person) {
        person = person.split("!")[0]; // Don't ask.
        if (!MessageHandler.StatusPrefixes.containsKey(channel)) return person;
        Map<String, IRCUserModes> channelMap = MessageHandler.StatusPrefixes.get(channel);
        if (!channelMap.containsKey(person)) return person;
        return switch (channelMap.get(person)) {
            case OWNER -> "§e~§r";
            case ADMIN -> "§6&§r";
            case OPERATOR -> "§a@§r";
            case HALF_OPERATOR -> "§1%§r";
            case VOICE -> "§b+§r";
            case NONE -> person;
        };
    }
}
