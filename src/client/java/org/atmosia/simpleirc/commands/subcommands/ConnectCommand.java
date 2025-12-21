package org.atmosia.simpleirc.commands.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.atmosia.simpleirc.ChatUtils;
import org.atmosia.simpleirc.Main;
import org.atmosia.simpleirc.MainClient;
import org.atmosia.simpleirc.IRCNetwork;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class ConnectCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> Register(LiteralArgumentBuilder<FabricClientCommandSource> command) {
        command.then(ClientCommandManager.literal("connect").executes(ConnectCommand::Connect)
                .then(argument("server_ip", StringArgumentType.string())
                        .then(argument("server_port", IntegerArgumentType.integer(0, 65535))
                                .then(argument("channel password", StringArgumentType.greedyString())
                                        .executes(ConnectCommand::ConnectWithArgs))
                                )
                        )
                );
        return command;
    }

    private static int ConnectWithArgs(CommandContext<FabricClientCommandSource> ctx) {
        var server = ctx.getArgument("server_ip", String.class);
        var serverPort = ctx.getArgument("server_port", Integer.class);
        var channelString = ctx.getArgument("channel", String.class);
        var channel = channelString.split(" ")[0];
        var channelPassword = channelString.split(" ")[1];
        MainClient.irc = new IRCNetwork(server, serverPort, channel, channelPassword, Main.getSettings().verbosity);
        MainClient.irc.Connect();
        return 0;
    }


    private static int Connect(CommandContext<FabricClientCommandSource> ctx) {
        try {
            MainClient.Connect();
        }
        catch (IllegalStateException e) {
            ChatUtils.Error(e);
        }
        return 0;
    }
}
