package org.atmosia.simpleirc.commands.subcommands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.atmosia.simpleirc.*;
import org.atmosia.simpleirc.commands.SimpleIrcCommand;
import org.atmosia.simpleirc.commands.suggestions.VerbositySuggestionProvider;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class ConfigCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> Register(LiteralArgumentBuilder<FabricClientCommandSource> command) {
        command
                .then(ClientCommandManager.literal("config").executes(ConfigCommand::ShowConfig)
                        .then(ClientCommandManager.literal("server")
                                .then(argument("value", StringArgumentType.string())
                                        .executes(ctx -> EditConfig(ctx, "server"))
                                )
                )
                        .then(ClientCommandManager.literal("port")
                                .then(argument("int_value", IntegerArgumentType.integer(0, 65535))
                                        .executes(ctx -> EditConfig(ctx, "port"))
                                )
                )
                        .then(ClientCommandManager.literal("channel")
                                .then(argument("value", StringArgumentType.string())
                                        .executes(ctx -> EditConfig(ctx, "channel"))
                                )
                )
                        .then(ClientCommandManager.literal("channel_password")
                                .then(argument("value", StringArgumentType.string())
                                        .executes(ctx -> EditConfig(ctx, "channel_password"))
                                )
                )
                        .then(ClientCommandManager.literal("backup_nickname")
                                .then(argument("value", StringArgumentType.string())
                                        .executes(ctx -> EditConfig(ctx, "backup_nickname"))
                                )
                )
                        .then(ClientCommandManager.literal("autoconnect")
                                .then(argument("bool_value", BoolArgumentType.bool())
                                        .executes(ctx -> EditConfig(ctx, "autoconnect"))
                                )
                )
                        .then(ClientCommandManager.literal("verbosity")
                                .then(argument("value", StringArgumentType.string())
                                        .suggests(new VerbositySuggestionProvider())
                                        .executes(ctx -> EditConfig(ctx, "verbosity"))
                                )
                )
        );
        return command;
    }
    private static int EditConfig(CommandContext<FabricClientCommandSource> context, String option) {
        switch (option) {
            case "server" -> {
                var server = context.getArgument("value", String.class);
                Main.getSettings().ip = server;
                ChatUtils.Notify("Set config option §7Server IP§r to '§7" + server + "§r'");
            }
            case "port" -> {
                var port = context.getArgument("int_value", Integer.class);
                Main.getSettings().port = port;
                ChatUtils.Notify("Set config option §7Port§r to §7" + port + "§r");
            }
            case "channel" -> {
                var channel = context.getArgument("value", String.class);
                Main.getSettings().channel = channel;
                ChatUtils.Notify("Set config option §7Channel§r to '§7" + channel + "§r'");
            }
            case "channel_password" -> {
                var password = context.getArgument("value", String.class);
                Main.getSettings().password = password;
                ChatUtils.Notify("Set config option §7Password§r to '§7" + password + "§r'");
            }
            case "backup_nickname" -> {
                var backup_nick = context.getArgument("value", String.class);
                Main.getSettings().backupnick = backup_nick;
                ChatUtils.Notify("Set config option §7Backup Nickname§r to '§7" + backup_nick + "§r'");
            }
            case "autoconnect" -> {
                var autoconnect = context.getArgument("bool_value", Boolean.class);
                Main.getSettings().autoconnect = autoconnect;
                ChatUtils.Notify("Set the config option §7Auto Connection§r to " + (autoconnect ? "§aTrue" : "§cFalse"));
            }
            case "verbosity" -> {
                var verbosityString = context.getArgument("value", String.class);
                var verbosity = IrcVerbosity.NORMAL;
                verbosity = switch (verbosityString.toLowerCase()) {
                    case "raw" -> IrcVerbosity.RAW;
                    case "verbose" -> IrcVerbosity.VERBOSE;
                    case "normal" -> IrcVerbosity.NORMAL;
                    case "quiet" -> IrcVerbosity.QUIET;
                    default -> verbosity;
                };
                Main.getSettings().verbosity = verbosity;
                ChatUtils.Notify("Set config option §7Verbosity§r to §7" + verbosityString.toUpperCase() + "§r");
                MainClient.irc.SetVerbosity(verbosity);
            }
        }
        return 0;
    }

    private static int ShowConfig(CommandContext<FabricClientCommandSource> ctx) {
        var client = ctx.getSource().getClient();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            ctx.getSource().sendError(Text.literal("Got interrupted while opening the config screen. Try again"));
        }
        client.send(() -> client.setScreen(AutoConfig.getConfigScreen(Ircgroup.class, client.currentScreen).get()));
        return 1;
    }
}
