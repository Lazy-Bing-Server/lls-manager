package com.plusls.llsmanager.mcdrCommand;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.command.Command;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Singleton
public class MCDReforgedCommand implements Command {
    @Inject
    LlsManager llsManager;

    private final ArrayList<CommandMeta> registeredCommands = new ArrayList<>();

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsMCDReforgedRegisterCommand = createSubCommand().build();
        return new BrigadierCommand(llsMCDReforgedRegisterCommand);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createSubCommand() {
        return LiteralArgumentBuilder
                .<CommandSource>literal("mcdr").requires(
                        commandSource -> commandSource instanceof ConsoleCommandSource
                ).requires(contextManager -> llsManager.config.getMcdrCommandSuggestion()).then(LiteralArgumentBuilder.<CommandSource>literal("register").then(
                        RequiredArgumentBuilder.<CommandSource, String>argument("data", StringArgumentType.greedyString())
                                .executes(this))
                );
    }

    @Override
    public int run(CommandContext<CommandSource> commandContext) throws CommandSyntaxException {
        CommandManager commandManager = llsManager.commandManager;

        try {
            for (CommandMeta commandMeta: registeredCommands) {
                commandManager.unregister(commandMeta);
            }
            registeredCommands.clear();
        } catch (Exception exception) {
            exception.printStackTrace();
            return 0;
        }

        try {
            JSONObject jsonObject = JSON.parseObject(StringArgumentType.getString(commandContext, "data"));
            JSONObject[] var14 = (JSONObject[])jsonObject.getJSONArray("data").toArray(JSONObject.class, new JSONReader.Feature[0]);

            for (JSONObject nodeJsonObject : var14) {
                Node node = new Node(nodeJsonObject);
                BrigadierCommand brigadierCommand = node.createBrigadierCommand(llsManager);
                CommandMeta commandMeta = commandManager.metaBuilder(brigadierCommand).build();
                this.registeredCommands.add(commandMeta);
                commandManager.register(commandMeta, brigadierCommand);
            }
        } catch (Exception var11) {
            var11.printStackTrace();
            return 0;
        }
        llsManager.logger.info("Updated MCDReforged command tree suggestion successfully");
        return 1;
    }
}
