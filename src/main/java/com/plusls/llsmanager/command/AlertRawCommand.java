package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.Optional;


public class AlertRawCommand {
    public static void register(LlsManager llsManager) {
        llsManager.commandManager.register(createBrigadierCommand(llsManager));
    }

    private static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LiteralCommandNode<CommandSource> alertRawNode = LiteralArgumentBuilder.<CommandSource>literal("alertraw")
                .requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .then(LiteralArgumentBuilder.<CommandSource>literal("@a")
                        .then(getSendComponentBuilder(llsManager).executes(
                                context -> {
                                    Component textComponent = GsonComponentSerializer.gson().deserialize(context.getArgument("component", String.class));
                                    for (Player player : llsManager.server.getAllPlayers()) {
                                        player.sendMessage(textComponent);
                                    }
                                    return 0;
                                }
                        )))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("target", StringArgumentType.word())
                        .then(getSendComponentBuilder(llsManager).executes(
                                context -> {
                                    Component textComponent = GsonComponentSerializer.gson().deserialize(context.getArgument("component", String.class));
                                    String playerName = context.getArgument("target", String.class);
                                    Optional<Player> player = llsManager.server.getPlayer(playerName);
                                    if (player.isPresent()) {
                                        player.get().sendMessage(textComponent);
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                        ))
                ).build();
        return new BrigadierCommand(alertRawNode);
    }

    private static RequiredArgumentBuilder<CommandSource, String> getSendComponentBuilder(LlsManager llsManager) {
        return RequiredArgumentBuilder.argument("component", StringArgumentType.greedyString());
    }
}
