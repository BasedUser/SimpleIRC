package org.atmosia.simpleirc.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
            ChatUtils.message("§aConnected to server: §e" + MainClient.irc.ip() + "§a, channels: §e" + MainClient.irc.channels());
            // TODO: refactor to not use ChatUtils.message(). Maybe .info()?
        }
        else
        {
            ChatUtils.Info("Not connected to a server.");
        }
        return 0;
    }
}
