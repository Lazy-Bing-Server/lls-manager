package com.plusls.llsmanager.mcdrCommand;

import com.alibaba.fastjson2.JSONObject;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;

public class Node {
    private final String name;
    private final String type;
    private final ArrayList<Node> children = new ArrayList<>();

    public Node(JSONObject jsonObject) {
        name = jsonObject.getString("name");
        type = jsonObject.getString("type");
        for (JSONObject child : jsonObject.getJSONArray("children").toArray(JSONObject.class)) {
            children.add(new Node(child));
        }
    }

    public String getName() {
        return name;
    }

    public BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        return new BrigadierCommand(getRootArgumentBuilder(llsManager).build());
    }

    public LiteralArgumentBuilder<CommandSource> getRootArgumentBuilder(LlsManager llsManager) {
        return (LiteralArgumentBuilder<CommandSource>) getArgumentBuilder(llsManager).executes(
                commandContext -> {
                    Player player = (Player) commandContext.getSource();
                    String username = player.getUsername();
                    String message = commandContext.getInput();
                    player.getCurrentServer().ifPresent((serverConnection) -> {
                        llsManager.logger.info(String.format("[%s] <%s> %s", serverConnection.getServerInfo().getName(), username, message));
                    });
                    return 0;
                }
        );
    }

    public ArgumentBuilder<CommandSource, ?> getArgumentBuilder(LlsManager llsManager) {
        ArgumentBuilder<CommandSource, ?> argumentBuilder = switch (type) {
            case "LITERAL" -> LiteralArgumentBuilder.literal(name);
            case "INTEGER" -> RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer());
            case "FLOAT" -> RequiredArgumentBuilder.argument(name, DoubleArgumentType.doubleArg());
            case "QUOTABLE_TEXT" -> RequiredArgumentBuilder.argument(name, StringArgumentType.string());
            case "GREEDY_TEXT" -> RequiredArgumentBuilder.argument(name, StringArgumentType.greedyString());
            default ->
                // NUMBER, TEXT, BOOLEAN, ENUMERATION, etc...
                    RequiredArgumentBuilder.argument(name, StringArgumentType.word());
        };
        argumentBuilder.executes(
                commandContext -> {
                    Player player = (Player) commandContext.getSource();
                    String username = player.getUsername();
                    String message = commandContext.getInput();
                    player.getCurrentServer().ifPresent((serverConnection) -> {
                        llsManager.logger.info(String.format("[%s] <%s> %s", serverConnection.getServerInfo().getName(), username, message));
                    });
                    return 1;
                }
        );
        for (Node child : children) {
            argumentBuilder.then(child.getArgumentBuilder(llsManager));
        }
        return argumentBuilder;
    }
}
