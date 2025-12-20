package org.atmosia.simpleirc.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.atmosia.simpleirc.ChatUtils;
import org.atmosia.simpleirc.MainClient;
import org.atmosia.simpleirc.commands.suggestions.ConnectedChannelsAndMcSuggester;
import org.atmosia.simpleirc.commands.suggestions.ConnectedChannelsSuggester;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class SetPrefixForChatCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> Register(LiteralArgumentBuilder<FabricClientCommandSource> command) {
        command.then(ClientCommandManager.literal("setchannelprefix")
                .then(argument("channel", StringArgumentType.string())
                        .suggests(new ConnectedChannelsSuggester())
                        .then(argument("prefix", StringArgumentType.greedyString())
                                .executes(SetPrefixForChatCommand::SetPrefix))))
                .then(ClientCommandManager.literal("defaultchannel")
                        .then(argument("channel", StringArgumentType.string())
                                .suggests(new ConnectedChannelsAndMcSuggester())
                                .executes(SetPrefixForChatCommand::SetDefaultChannel)))
                ;
        return command;
    }
    private static int SetPrefix(CommandContext<FabricClientCommandSource> context) {
        if (MainClient.irc == null || !MainClient.irc.isConnected()) {
            context.getSource().sendError(Text.literal("You are not connected to IRC server."));
            return -1;
        }
        var channel = context.getArgument("channel", String.class);
        String prefix = StringArgumentType.getString(context, "prefix");
        if (prefix.length() != 1) {
            context.getSource().sendError(Text.literal("Prefix must be a single character."));
            return -1;
        }
        if (MainClient.irc.GetChannel(channel) == null) {
            context.getSource().sendError(Text.literal("You aren't connected to such channel."));
            return -1;
        }
        Character prefixChar = prefix.charAt(0);
        MainClient.irc.AddChannelByPrefix(prefixChar, MainClient.irc.GetChannel(channel));

        ChatUtils.Notify("Added prefix §7" + prefixChar + "§r to channel §7" + channel);

        return 0;
    }
    private static int SetDefaultChannel(CommandContext<FabricClientCommandSource> context) {
        if (MainClient.irc == null || !MainClient.irc.isConnected()) {
            context.getSource().sendError(Text.literal("You are not connected to IRC server."));
            return -1;
        }
        var channel = context.getArgument("channel", String.class);
        if (MainClient.irc.GetChannel(channel) == null && !channel.equals("minecraft_chat")) {
            context.getSource().sendError(Text.literal("You aren't connected to such channel."));
            return -1;
        }
        else if (channel.equals("minecraft_chat")) {
            MainClient.DefaultToMinecraftChat = true;
        } else {
            MainClient.DefaultToMinecraftChat = false;
            MainClient.irc.SetPrimaryChannel(channel);
        }
        ChatUtils.Notify("Set channel <gray>" + channel + "</gray> as the primary channel.");
        return 0;
    }
}
