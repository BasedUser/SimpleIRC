package org.atmosia.simpleirc.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;

import org.atmosia.simpleirc.*;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatManager {


    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo info) 
    {
        if (message == null) {
            return;
        }
        boolean skipIrcStuff = MainClient.irc == null || MainClient.DefaultToMinecraftChat || message.charAt(0) == '!';
        if (skipIrcStuff) {
            Main.LOGGER.info("Sent message to normal chat, since one condition isn't satisfied");
        }
        else {
            Main.LOGGER.info("Sending message to IRC chat");
            var channel = MainClient.irc.GetChannelByPrefix(message.charAt(0));
            if (channel != null) {
                String text = message.substring(1);
                MainClient.irc.SendMessage(channel.Name, text);
                String selfNick = MainClient.irc.nickname();
                ChatUtils.IRCMessageTemplates.Message(selfNick, channel.Name, text);
                info.cancel();
            }
            else if (MainClient.irc != null && MainClient.irc.isConnected()
                    && !MainClient.DefaultToMinecraftChat
                    && !String.valueOf(message.charAt(0)).equals("/")
                    && !MainClient.irc.channels().isEmpty()) {
                MainClient.irc.SendMessage("", message);
                String currentChannel = MainClient.irc.GetPrimaryChannel();
                String selfNick = MainClient.irc.nickname();
                ChatUtils.IRCMessageTemplates.Message(selfNick, currentChannel, message);
                info.cancel();
            }
        }


        // Normal chat / IRC bridging

			// Bound prefix to specific channel
//			if (MainClient.irc != null && MainClient.irc.isConnected() && message.length() > 0) {
//				char key = message.charAt(0);
//				if (channelBindings.containsKey(key)) {
//					String targetChannel = channelBindings.get(key);
//					if (targetChannel == null || targetChannel.isEmpty()) {
//						ChatUtils.message("§cNo channel bound to prefix '" + key + "'.");
//						info.cancel();
//						return;
//					}
//
//					// bare prefix: rebind default channel
//					if (message.length() == 1) {
//						MainClient.irc.SetPrimaryChannel(targetChannel);
//						ChatUtils.message("§9Default IRC channel set to §e" + targetChannel);
//						info.cancel();
//						return;
//					}
//
//					// prefixed message -> send once to bound channel
//					String text = message.substring(1);
//					MainClient.irc.SendMessage(targetChannel, text);
//					String selfNick = MainClient.irc.nickname();
//					ChatUtils.IRCMessageTemplates.Message(selfNick, targetChannel, text);
//					info.cancel();
//					return;
//				}
//			}

			// "!" global -> normal MC chat only
//			if (MainClient.irc != null && MainClient.irc.isConnected() && !message.isEmpty()
//				&& String.valueOf(message.charAt(0)).equals(global)) {
//
//				// "!" alone: switch default to Minecraft chat
//				if (message.length() == 1) {
//					defaultToMinecraft = true;
//					ChatUtils.message("§9Default chat set to §eMinecraft§9 (use '" + prefix + "' alone to go back to IRC)");
//					info.cancel();
//					return;
//				}
//
//				// "!" + text: send to game chat once - technically a hack
//				oneOffMinecraft = true;
//				ChatUtils.sudomessage(message.substring(1));
//				oneOffMinecraft = false;
//				info.cancel();
//				return;
//			}
//			// default: send to current IRC channel if connected and not a command
//			else if (MainClient.irc != null && MainClient.irc.isConnected()
//				&& !defaultToMinecraft
//				&& !String.valueOf(message.charAt(0)).equals("/"))
//			{
//				MainClient.irc.SendMessage("", message);
//				String currentChannel = MainClient.irc.GetPrimaryChannel();
//				String selfNick = MainClient.irc.nickname();
//				ChatUtils.IRCMessageTemplates.Message(selfNick, currentChannel, message);
//				info.cancel();
//				return;
//		}
    }
}
