package org.atmosia.simpleirc.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.atmosia.simpleirc.ChatUtils;
import org.atmosia.simpleirc.MainClient;

public class StatusCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> Register(LiteralArgumentBuilder<FabricClientCommandSource> command) {
        command.then(ClientCommandManager.literal("status").executes(StatusCommand::Status));
        return command;
    }
    public static int Status(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        if(MainClient.irc!=null && MainClient.irc.isConnected())
        {
            Component message = MiniMessage.miniMessage().deserialize(
                    "<green>Connected to server:</green> <yellow>" + MainClient.irc.ip() + "</yellow><green>, channels: <yellow> " + MainClient.irc.channels()
            );
            ChatUtils.message(message);
            // TODO: refactor to not use ChatUtils.message(). Maybe .info()?
        }
        else
        {
            ChatUtils.Info("Not connected to a server.");
        }
        return 0;
    }
}
