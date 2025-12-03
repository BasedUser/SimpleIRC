package org.atmosia.simpleirc.mixin.client;

import java.util.StringTokenizer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import java.util.HashMap;
import java.util.Map;

import org.atmosia.simpleirc.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatManager {
    private String prefix = "@";
    private String global = "!";
    
    private boolean nickrequired = false;
    private boolean takename = true;
    
    private String ip = "";
    private String nick = "";
    private String channel = "";
    private String password = "";
    // default IRCs port: 6697
    private Integer port = 6697;
	
	// whether unprefixed messages should go to Minecraft (true) or IRC (false) when connected
	private boolean defaultToMinecraft = false;
	private boolean oneOffMinecraft = false; // don't ask

    // prefix -> channel name (#, $, etc)
    private Map<Character, String> channelBindings = new HashMap<Character, String>();

    // desired verbosity (applied to new IRCHandler instances)
    private IrcVerbosity pendingVerbosity = IrcVerbosity.NORMAL;
    
    
    private String help = "\n\n\u00A7cHELP \u00A7f\n\n"
            + prefix + "status - check the irc connection status\n\n"
            + prefix + "connect - connect to the irc server \n\u00A7eusage: "+prefix+"connect serverip;port;channel;password \n(password is optional)\u00A7f\n\n"
            + prefix + "disconnect - disconnect from the irc server\n\n"
            + prefix + "set - list prefix bindings\n"
            + prefix + "set <key> <channel> - bind a chat prefix to an IRC channel\n"
            + prefix + "set <key> - delete an existing binding\n\n"
            + prefix + "raw <command> - send a raw IRC line directly\n\n"
            + prefix + "verbosity <QUIET/NORMAL/VERBOSE/RAW> - set IRC verbosity\n\n"
            + prefix + "join <#channel> [key] - join a channel\n"
            + prefix + "part [#channel] [reason] - part from a channel (default: current)\n\n"
            + "\u00A79>>\u00A7c When an IRC connection is active, you need to use the prefix '"+global+"' to write in-game \u00A79<<\u00A7f\n\n";
    
    
    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo info) 
    {
        if (message == null) {
            return;
        }

        if(nickrequired)
        {
            nick = message;
            nickrequired = false;
            takename = false;
            
            ChatUtils.sudomessage(prefix+"connect "+ip+";"+port+";"+channel+";"+password);
            
            info.cancel();
            return;
        }

        if (!oneOffMinecraft) {
			// HELP
			if (message.equals(prefix + "help") || message.equals(prefix + "h"))
			{
				ChatUtils.message(help);
				info.cancel();
				return;
			}
			// STATUS
			else if(message.equals(prefix + "status"))
			{
				if(MainClient.irc!=null && MainClient.irc.isOpen())
				{
					ChatUtils.message("\u00A7aConnected to server: \u00A7e" + MainClient.irc.getServer() + "\u00A7a, channel: \u00A7e" + MainClient.irc.getChannelname());
				}
				else
				{
					ChatUtils.message("\u00A7cDisconnected");
				}
				info.cancel();
				return;
			}
			// JOIN
			else if (message.startsWith(prefix + "join"))
			{
				handleJoin(message);
				info.cancel();
				return;
			}
			// PART
			else if (message.startsWith(prefix + "part"))
			{
				handlePart(message);
				info.cancel();
				return;
			}
			// SET prefix binding
			else if (message.startsWith(prefix + "set"))
			{
				handleSetCommand(message);
				info.cancel();
				return;
			}
			// RAW
			else if (message.startsWith(prefix + "raw"))
			{
				handleRawCommand(message);
				info.cancel();
				return;
			}
			// VERBOSITY
			else if (message.startsWith(prefix + "verbosity"))
			{
				handleVerbosity(message);
				info.cancel();
				return;
			}
			// CONNECT
			else if(message.contains(prefix + "connect"))
			{
				handleConnect(message);
				info.cancel();
				return;
			}
			// DISCONNECT
			else if(message.equals(prefix + "disconnect"))
			{
				if(MainClient.irc!=null && MainClient.irc.isOpen())
				{
					MainClient.irc.closeConnection();
					ChatUtils.message("\u00A7cDisconnected");
				}
				
				info.cancel();
				return;
			}
			// "@": switch default back to IRC primary channel
			else if (message.equals(prefix)) {
				if (MainClient.irc != null && MainClient.irc.isOpen()) {
					defaultToMinecraft = false;
					if (channel != null) {
						MainClient.irc.setChannelname(channel);
						ChatUtils.message("\u00A79Default chat set to \u00A7eIRC\u00A79 channel \u00A7e" + channel);
					}
				} else {
					ChatUtils.message("\u00A7cNot connected to IRC. Did you mean @help?");
				}
				info.cancel();
				return;
			}

			// Normal chat / IRC bridging

			// Bound prefix to specific channel
			if (MainClient.irc != null && MainClient.irc.isOpen() && message.length() > 0) {
				char key = message.charAt(0);
				if (channelBindings.containsKey(key)) {
					String targetChannel = channelBindings.get(key);
					if (targetChannel == null || targetChannel.isEmpty()) {
						ChatUtils.message("\u00A7cNo channel bound to prefix '" + key + "'.");
						info.cancel();
						return;
					}

					// bare prefix: rebind default channel
					if (message.length() == 1) {
						MainClient.irc.setChannelname(targetChannel);
						ChatUtils.message("\u00A79Default IRC channel set to \u00A7e" + targetChannel);
						info.cancel();
						return;
					}

					// prefixed message -> send once to bound channel
					String text = message.substring(1);
					MainClient.irc.sendGroupMsg(targetChannel, text);
					String selfNick = MainClient.irc.getNick();
					String formattedSelf = MainClient.irc.getFormattedNick(selfNick, targetChannel);
					ChatUtils.message("\u00A79" + targetChannel + "\u00A7r | <" + formattedSelf + "\u00A7r> " + text);
					info.cancel();
					return;
				}
			}

			// "!" global -> normal MC chat only
			if (MainClient.irc != null && MainClient.irc.isOpen() && message.length() > 0
				&& String.valueOf(message.charAt(0)).equals(global)) {

				// "!" alone: switch default to Minecraft chat
				if (message.length() == 1) {
					defaultToMinecraft = true;
					ChatUtils.message("\u00A79Default chat set to \u00A7eMinecraft\u00A79 (use '" + prefix + "' alone to go back to IRC)");
					info.cancel();
					return;
				}

				// "!" + text: send to game chat once - technically a hack
				oneOffMinecraft = true;
				ChatUtils.sudomessage(message.substring(1));
				oneOffMinecraft = false;
				info.cancel();
				return;
			}
			// default: send to current IRC channel if connected and not a command
			else if (MainClient.irc != null && MainClient.irc.isOpen()
				&& !defaultToMinecraft
				&& !String.valueOf(message.charAt(0)).equals("/"))
			{
				MainClient.irc.sendGroupMsg(message);
				String currentChannel = MainClient.irc.getChannelname();
				String selfNick = MainClient.irc.getNick();
				String formattedSelf = MainClient.irc.getFormattedNick(selfNick, currentChannel);
				ChatUtils.message("\u00A79" + currentChannel + "\u00A7r | <" + formattedSelf + "\u00A7r> " + message);
				info.cancel();
				return;
			}
		}
    }

    private void handleConnect(String message) {
        if(message.equals(prefix + "connect"))
        {
            if(!"".equals(Main.getSettings().ip) && !"".equals(Main.getSettings().channel) && !"".equals(Main.getSettings().backupnick))
            {
                if(MainClient.irc != null && MainClient.irc.getSocket()!=null)
                {
                    if(MainClient.irc.isOpen())
                    {
                        MainClient.irc.closeConnection();
                    }    
                }
                ip = Main.getSettings().ip;
                nick = ChatUtils.getUsername();
				channel = Main.getSettings().channel;
				port = Main.getSettings().port;
				password = Main.getSettings().password;
				
                if(Character.isDigit(nick.charAt(0)))
                    nick = Main.getSettings().backupnick;
                
                MainClient.irc = new IRCHandler(ip, nick, channel, password, port);
				MainClient.irc.setVerbosity(pendingVerbosity);
				MainClient.irc.startConn();
            }
            else
            {
                ChatUtils.message("\u00A7cError\n"
                          + "\u00A7eMissing config fields or syntax error\n"
                          + "\u00A7eusage: "+prefix+"connect serverip;port;channel;password \n(password is optional)\u00A7f");
                System.out.println("Unable to connect: missing config fields or syntax error");
            }
        }
        else
        {
            StringTokenizer st = new StringTokenizer(message," ");
            
            st.nextToken();
            String[] fields = st.nextToken().split(";");

            if(fields.length<2)
            {
                ChatUtils.message("\u00A7cOne or more mandatory fields are missing.");
            }
            else
            {            
                ip = fields[0];
                if(takename)
                {
                    nick = ChatUtils.getUsername();
                }
                
                try {
                    port = Integer.parseInt(fields[1]);
                }
                catch(Exception e) {
                    ChatUtils.message("\u00A7cError: port must be valid, falling back to default (6697).");
                    port = 6697;
                }
                channel = fields[2];
                if(fields.length>3)
                {
                    password = fields[3];
                }
                
                if(port>=65536 || port < 1)
                {
                    ChatUtils.message("\u00A7cError: port must be valid, falling back to default (6697).");
                    port = 6697;
                }

                if(!Character.isDigit(nick.charAt(0)))
                {
                    takename = true;
                    if(!"".equals(ip) && !"".equals(nick) && !"".equals(channel))
                    {
                        if(MainClient.irc != null && MainClient.irc.getSocket()!=null)
                        {
                            if(MainClient.irc.isOpen())
                            {
                                MainClient.irc.closeConnection();
                            }    
                        }
                        
                        MainClient.irc = new IRCHandler(ip,nick,channel,password,port);
						MainClient.irc.setVerbosity(pendingVerbosity);
						MainClient.irc.startConn();
                    }
                    else
                    {
                        ChatUtils.message("\u00A7cOne or more mandatory fields are missing.");
                    }
                }
                else
                {
                    nickrequired=true;
                    ChatUtils.message("\u00A76Error: Your name starts with a number. Choose a new nickname:");
                }
            }
        }
    }

    private void handleSetCommand(String message) {
        // @set / @set <key> / @set <key> <channel>

        String trimmed = message.trim();
		String base = prefix + "set";

		if (trimmed.equals(base)) {
			// list bindings
			ChatUtils.message("\u00A7aCurrent prefix bindings:");

			// Reserved, config-defined bindings:
			// global '!' → Minecraft chat default route
			ChatUtils.message("  \u00A7e" + global + "\u00A7r -> \u00A79Minecraft chat \u00A77(unmodifiable)");

			// primary '@' → primary IRC channel (if known)
			if (channel != null && !channel.isEmpty()) {
				ChatUtils.message("  \u00A7e" + prefix.charAt(0) + "\u00A7r -> \u00A79primary IRC channel \u00A7e" + channel + " \u00A77(unmodifiable)");
			} else {
				ChatUtils.message("  \u00A7e" + prefix.charAt(0) + "\u00A7r -> \u00A79IRC (default route toggle) \u00A77(unmodifiable)");
			}

			if (channelBindings.isEmpty()) {
				ChatUtils.message("  \u00A77(no custom bindings)");
			} else {
				for (Map.Entry<Character, String> e : channelBindings.entrySet()) {
					ChatUtils.message("  \u00A7e" + e.getKey() + "\u00A7r -> \u00A79" + e.getValue());
				}
			}
			return;
		}

        String[] parts = message.split(" ", 3);
		if (parts.length < 2) {
			ChatUtils.message("\u00A7cUsage: " + base + " <key> <channel> OR " + base + " <key> OR " + base);
			return;
		}

		String keyStr = parts[1];
		if (keyStr.length() != 1) {
			ChatUtils.message("\u00A7cKey must be a single character (e.g. #, $, ...)");
			return;
		}

		char key = keyStr.charAt(0);

		// reserved: '!' (global) and '@' (prefix)
		if (key == global.charAt(0) || key == prefix.charAt(0)) {
			ChatUtils.message("\u00A7cPrefix '\u00A7e" + key + "\u00A7c' is reserved and cannot be modified.");
			return;
		}

        // delete binding
        if (parts.length == 2) {
            if (channelBindings.remove(key) != null) {
                ChatUtils.message("\u00A7aUnbound prefix '\u00A7e" + key + "\u00A7a'.");
            } else {
                ChatUtils.message("\u00A7cNo binding found for prefix '" + key + "'.");
            }
            return;
        }

        // bind prefix to a channel
        String channel = parts[2].trim();
        if (channel.isEmpty()) {
            ChatUtils.message("\u00A7cChannel cannot be empty.");
            return;
        }
        channelBindings.put(key, channel);
        ChatUtils.message("\u00A7aBound prefix '\u00A7e" + key + "\u00A7a' to channel '\u00A7e" + channel + "\u00A7a'.");
    }

    private void handleRawCommand(String message) {
        // @raw <command>
        if (MainClient.irc == null || !MainClient.irc.isOpen()) {
            ChatUtils.message("\u00A7cNot connected to IRC.");
            return;
        }

        String base = prefix + "raw";
        if (message.equals(base)) {
            ChatUtils.message("\u00A7cUsage: " + base + " <raw IRC command>");
            return;
        }

        String raw = message.substring(base.length()).trim();
        if (raw.isEmpty()) {
            ChatUtils.message("\u00A7cUsage: " + base + " <raw IRC command>");
            return;
        }

        MainClient.irc.sendRawLine(raw);
    }

    private void handleVerbosity(String message) {
        // @verbosity <QUIET|NORMAL|VERBOSE|RAW>
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            ChatUtils.message("\u00A7cUsage: " + prefix + "verbosity <QUIET/NORMAL/VERBOSE/RAW>");
            return;
        }

        String level = parts[1].trim().toUpperCase();
        try {
            IrcVerbosity v = IrcVerbosity.valueOf(level);
            pendingVerbosity = v;
            if (MainClient.irc != null) {
                MainClient.irc.setVerbosity(v);
            }
            ChatUtils.message("\u00A7aIRC verbosity set to \u00A7e" + v.name());
        } catch (IllegalArgumentException e) {
            ChatUtils.message("\u00A7cInvalid verbosity. Use QUIET, NORMAL, VERBOSE, RAW.");
        }
    }

    private void handleJoin(String message) {
        if (MainClient.irc == null || !MainClient.irc.isOpen()) {
            ChatUtils.message("\u00A7cNot connected to IRC.");
            return;
        }

        String[] parts = message.split(" ", 3);
        if (parts.length < 2) {
            ChatUtils.message("\u00A7cUsage: " + prefix + "join <#channel> [key]");
            return;
        }

        String chan = parts[1];
        String key = (parts.length >= 3) ? parts[2] : null;

        if (key != null && !key.isEmpty()) {
            MainClient.irc.sendRawLine("JOIN " + chan + " " + key);
        } else {
            MainClient.irc.join(chan);
        }

        ChatUtils.message("\u00A79Joining \u00A7e" + chan + "\u00A79...");
    }

    private void handlePart(String message) {
        if (MainClient.irc == null || !MainClient.irc.isOpen()) {
            ChatUtils.message("\u00A7cNot connected to IRC.");
            return;
        }

        String[] parts = message.split(" ", 3);
        String chan;
        String reason = "leaving";

        if (parts.length >= 2) {
            chan = parts[1];
            if (parts.length >= 3) {
                reason = parts[2];
            }
        } else {
            chan = MainClient.irc.getChannelname();
        }

        if (chan == null || chan.isEmpty()) {
            ChatUtils.message("\u00A7cNo channel to part from.");
            return;
        }

        MainClient.irc.sendRawLine("PART " + chan + " :" + reason);
        ChatUtils.message("\u00A79Parting \u00A7e" + chan + "\u00A79 (" + reason + ")");
    }
}
