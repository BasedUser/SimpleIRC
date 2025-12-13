package org.atmosia.simpleirc.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.atmosia.simpleirc.MainClient;

import java.util.concurrent.CompletableFuture;

public class ConnectedChannelsSuggester implements SuggestionProvider<FabricClientCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
        for (var channel : MainClient.irc.channels()) {
            suggestionsBuilder.suggest(channel.Name.substring(1));
        }
        return suggestionsBuilder.buildFuture();
    }
}
