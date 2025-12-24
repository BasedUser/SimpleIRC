package org.atmosia.simpleirc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.atmosia.simpleirc.commands.subcommands.*;


public class SimpleIrcCommand {
    public static void Register() {
        ClientCommandRegistrationCallback.EVENT.register(SimpleIrcCommand::RegisterForReal);
    }
    private static void RegisterForReal(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        var command = ClientCommandManager.literal("simpleirc").executes(SimpleIrcCommand::SendHelp)
                .then(ClientCommandManager.literal("help").executes(SimpleIrcCommand::SendHelp));
        ConfigCommand.Register(command);
        ConnectCommand.Register(command);
        DisconnectCommand.Register(command);
        StatusCommand.Register(command);
        IRCBasicCommands.Register(command);
        SetPrefixForChatCommand.Register(command);
        dispatcher.register(command);
    }
    private static int SendHelp(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("SimpleIRC command help:"));
        return 1;
    }
}
