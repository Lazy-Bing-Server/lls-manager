package com.plusls.llsmanager.whitelist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.command.Command;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class LlsWhitelistCommand {

    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsSeenNode = LiteralArgumentBuilder
                .<CommandSource>literal("lbs_whitelist")
                .requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .executes(llsManager.injector.getInstance(StatusUtils.class)::whiteListStatus)
                .then(llsManager.injector.getInstance(AddCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(RemoveCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(HelpCommand.class).createSubCommand())
                .then(
                        LiteralArgumentBuilder.<CommandSource>literal("enable")
                                .executes(llsManager.injector.getInstance(StatusUtils.class)::enableWhitelist)
                )
                .then(
                        LiteralArgumentBuilder.<CommandSource>literal("disable")
                                .executes(llsManager.injector.getInstance(StatusUtils.class)::disableWhitelist)
                ).then(llsManager.injector.getInstance(QueryCommand.class).createSubCommand())
                .build();
        return new BrigadierCommand(llsSeenNode);
    }

    @Singleton
    private static class StatusUtils {

        @Inject
        LlsManager llsManager;

            public int enableWhitelist(CommandContext<CommandSource> commandContext) {
                CommandSource source = commandContext.getSource();
                if (llsManager.lbsWhiteList.isWhiteListEnabled()) {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.enable.already"));
                    return 0;
                }
                boolean value = llsManager.lbsWhiteList.setWhitelistStatus(true);
                if (!value) {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.enable.failure"));
                    return 0;
                }
                source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.enable.success"));
                return 1;
            }

            public int disableWhitelist(CommandContext<CommandSource> commandContext) {
                CommandSource source = commandContext.getSource();
                if (!llsManager.lbsWhiteList.isWhiteListEnabled()) {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.disable.already"));
                    return 0;
                }
                boolean value = llsManager.lbsWhiteList.setWhitelistStatus(false);
                if (!value) {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.disable.failure"));
                    return 0;
                }
                source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.disable.success"));
                return 1;
            }

        public int whiteListStatus(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();
            NamedTextColor color;
            boolean enabledFlag = llsManager.lbsWhiteList.isWhiteListEnabled();
            if (enabledFlag) {
                color = NamedTextColor.GREEN;
            } else {
                color = NamedTextColor.RED;
            }
            source.sendMessage(
                    Component.translatable("lls-manager.command.lbs_whitelist.status").args(
                            Component.text(String.valueOf(enabledFlag))
                                    .color(color)
                    )
            );
            return 1;
        }
    }

    @Singleton
    private static class AddCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("add")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("username/uuid", StringArgumentType.string())
                            .suggests(this::getUsernameSuggestions).executes(this)
                            );
        }

        public CompletableFuture<Suggestions> getUsernameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.playerSet.forEach(
                    username -> {
                        if (username.contains(builder.getRemaining())) {
                            builder.suggest(username);
                        }
                    }
            );
            return builder.buildFuture();
        }

        private int errorOccurred(CommandSource source, String username) {
            source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.add.failure")
                    .color(NamedTextColor.RED)
                    .args(TextUtil.getUsernameComponent(username))
            );
            return 0;
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            String username = commandContext.getArgument("username/uuid", String.class);
            CommandSource source = commandContext.getSource();
            @Nullable UUID playerUUID = UUIDUtil.getUUID(llsManager, username);
            if (playerUUID == null) {
                return errorOccurred(source, username);
            }

            if (llsManager.lbsWhiteList.isPlayerWhitelisted(playerUUID)) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.add.already_in_whitelist")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW),
                                Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)));
                return 0;
            } else {
                boolean saved = llsManager.lbsWhiteList.whitelistAdd(playerUUID);
                if (!saved) {
                    return errorOccurred(source, username);
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.add.success")
                            .color(NamedTextColor.GREEN)
                            .args(
                                    TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW),
                                    Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)
                            ));
                    return 1;
                }
            }
        }
    }

    @Singleton
    private static class RemoveCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("remove")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("username/uuid", StringArgumentType.string())
                            .suggests(this::getUsernameSuggestions)
                            .executes(this));
        }

        public CompletableFuture<Suggestions> getUsernameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.playerSet.forEach(
                    username -> {
                        if (username.contains(builder.getRemaining())) {
                            builder.suggest(username);
                        }
                    }
            );
            return builder.buildFuture();
        }

        private int errorOccurred(CommandSource source, String username) {
            source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.remove.failure")
                    .color(NamedTextColor.RED)
                    .args(TextUtil.getUsernameComponent(username))
            );
            return 0;
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            String username = commandContext.getArgument("username/uuid", String.class);
            CommandSource source = commandContext.getSource();
            UUID playerUUID = UUIDUtil.getUUID(llsManager, username);
            if (playerUUID == null) {
                return errorOccurred(source, username);
            }

            if (!llsManager.lbsWhiteList.isPlayerWhitelisted(playerUUID)) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.remove.not_in_whitelist")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW),
                                Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)));
                return 0;
            } else {
                boolean saved = llsManager.lbsWhiteList.whitelistRemove(playerUUID);
                if (!saved) {
                    return errorOccurred(source, username);
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_whitelist.remove.success")
                            .color(NamedTextColor.GREEN)
                            .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW),
                                    Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)));
                    return 1;
                }
            }
        }
    }

    @Singleton
    private static class QueryCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("query")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("username/uuid", StringArgumentType.string())
                            .suggests(this::getUsernameSuggestions).executes(this)
                    );
        }

        public CompletableFuture<Suggestions> getUsernameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.playerSet.forEach(
                    username -> {
                        if (username.contains(builder.getRemaining())) {
                            builder.suggest(username);
                        }
                    }
            );
            return builder.buildFuture();
        }

        private Component notWhitelisted(String target){
            return notWhitelisted(target, NamedTextColor.YELLOW);
        }

        private Component notWhitelisted(String target, NamedTextColor color) {
            return Component.translatable("lls-manager.command.lbs_whitelist.query.not").args(
                    Component.text(target).color(color)
            );
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            String username = commandContext.getArgument("username/uuid", String.class);
            CommandSource source = commandContext.getSource();
            @Nullable UUID playerUUID = null;
            try {
                playerUUID = UUID.fromString(username);
            } catch (IllegalArgumentException ignored) {}

            if (playerUUID == null) {
                @Nullable UUID onlineUUID = UUIDUtil.getOnlineUUIDFromUserName(username);
                @Nullable UUID offlineUUID = UUIDUtil.getOfflineUUIDFromUserName(username);
                boolean found = false;
                if (onlineUUID != null && llsManager.lbsWhiteList.isPlayerWhitelisted(onlineUUID)) {
                    found = true;
                    source.sendMessage(
                            Component.translatable("lls-manager.command.lbs_whitelist.query.username.online_in")
                                    .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW), Component.text(onlineUUID.toString()).color(NamedTextColor.GOLD))
                    );
                }
                if (offlineUUID != null && llsManager.lbsWhiteList.isPlayerWhitelisted(offlineUUID)) {
                    found = true;
                    source.sendMessage(
                            Component.translatable("lls-manager.command.lbs_whitelist.query.username.offline_in")
                                    .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW), Component.text(offlineUUID.toString()).color(NamedTextColor.GOLD))
                    );
                }
                if (!found) {
                    source.sendMessage(notWhitelisted(username));
                }
            } else {
                if (llsManager.lbsWhiteList.isPlayerWhitelisted(playerUUID)) {
                    source.sendMessage(
                            Component.translatable("lls-manager.command.lbs_whitelist.query.uuid.in").args(
                                    Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)
                            )
                    );
                } else {
                    source.sendMessage(notWhitelisted(playerUUID.toString(), NamedTextColor.GOLD));
                }
            }
        return 1;
        }
    }

    @Singleton
    private static class HelpCommand implements Command {

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("help").executes(this);
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();
            for (int i = 0; i < 6; ++i) {
                source.sendMessage(Component.translatable(String.format("lls-manager.command.lbs_whitelist.hint%d", i)));
            }
            return 1;
        }
    }
}
