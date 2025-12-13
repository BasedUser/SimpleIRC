package org.atmosia.simpleirc.commands.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.atmosia.simpleirc.MainClient;
import org.atmosia.simpleirc.classes.IRCChannel;
import org.atmosia.simpleirc.commands.suggestions.ConnectedChannelsSuggester;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class IRCBasicCommands {
    public static LiteralArgumentBuilder<FabricClientCommandSource> Register(LiteralArgumentBuilder<FabricClientCommandSource> command) {
        command.then(ClientCommandManager.literal("join")
                .then(argument("channel", StringArgumentType.string()).executes(IRCBasicCommands::Join)
                        .then(argument("password", StringArgumentType.string()).executes(IRCBasicCommands::JoinWithPass))))
                .then(ClientCommandManager.literal("part")
                        .then(argument("channel", StringArgumentType.string())
                                .suggests(new ConnectedChannelsSuggester())
                                .executes(IRCBasicCommands::Part)))
                .then(ClientCommandManager.literal("raw")
                        .then(argument("command", StringArgumentType.greedyString()).executes(IRCBasicCommands::Raw))
                );
        return command;
    }

    private static int Part(CommandContext<FabricClientCommandSource> context) {
        if (MainClient.irc == null || !MainClient.irc.isConnected()) {
            context.getSource().sendError(Text.literal("You are not connected to IRC server."));
            return -1;
        }
        var channel = context.getArgument("channel", String.class);
        if (channel.isEmpty()) {
            context.getSource().sendError(Text.literal("Please specify a channel."));
        }
        var ircChannel = MainClient.irc.GetChannel(channel);
        MainClient.irc.SendLine(ircChannel.Part("Leaving"));
        return 0;
    }

    private static int Raw(CommandContext<FabricClientCommandSource> context) {
        MainClient.irc.SendLine(context.getArgument("command", String.class));
        return 0;
    }

    private static int JoinWithPass(CommandContext<FabricClientCommandSource> context) {
        if (MainClient.irc == null || !MainClient.irc.isConnected()) {
            context.getSource().sendError(Text.literal("You are not connected to IRC server."));
            return -1;
        }
        var channel = context.getArgument("channel", String.class);
        var password = context.getArgument("password", String.class);
        MainClient.irc.AddChannel(new IRCChannel("#" + channel, password, true));
        return 0;
    }

    private static int Join(CommandContext<FabricClientCommandSource> context) {

        if (MainClient.irc == null || !MainClient.irc.isConnected()) {
            context.getSource().sendError(Text.literal("You are not connected to IRC server."));
            return -1;
        }
        var channel = context.getArgument("channel", String.class);

        MainClient.irc.AddChannel(new IRCChannel("#" + channel, "", true));
        return 0;
    }
}
