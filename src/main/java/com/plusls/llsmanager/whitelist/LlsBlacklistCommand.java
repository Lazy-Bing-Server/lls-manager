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
import javax.sound.midi.spi.SoundbankReader;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class LlsBlacklistCommand {

    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsSeenNode = LiteralArgumentBuilder
                .<CommandSource>literal("lbs_blacklist")
                .requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .executes(llsManager.injector.getInstance(StatusUtils.class)::blacklistStatus)
                .then(llsManager.injector.getInstance(AddCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(RemoveCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(HelpCommand.class).createSubCommand())
                .then(
                        LiteralArgumentBuilder.<CommandSource>literal("enable")
                                .executes(llsManager.injector.getInstance(StatusUtils.class)::enableBlacklist)
                )
                .then(
                        LiteralArgumentBuilder.<CommandSource>literal("disable")
                                .executes(llsManager.injector.getInstance(StatusUtils.class)::disableBlacklist)
                ).then(llsManager.injector.getInstance(QueryCommand.class).createSubCommand())
                .build();
        return new BrigadierCommand(llsSeenNode);
    }

    @Singleton
    private static class StatusUtils {

        @Inject
        LlsManager llsManager;

        public int enableBlacklist(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();
            if (llsManager.lbsWhiteList.isBlackListEnabled()) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.enable.already"));
                return 0;
            }
            boolean value = llsManager.lbsWhiteList.setBlacklistStatus(true);
            if (!value) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.enable.failure"));
                return 0;
            }
            source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.enable.success"));
            return 1;
        }

        public int disableBlacklist(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();
            if (!llsManager.lbsWhiteList.isBlackListEnabled()) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.disable.already"));
                return 0;
            }
            boolean value = llsManager.lbsWhiteList.setBlacklistStatus(false);
            if (!value) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.disable.failure"));
                return 0;
            }
            source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.disable.success"));
            return 1;
        }

        public int blacklistStatus(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();
            NamedTextColor color;
            boolean enabledFlag = llsManager.lbsWhiteList.isBlackListEnabled();
            if (enabledFlag) {
                color = NamedTextColor.GREEN;
            } else {
                color = NamedTextColor.RED;
            }
            source.sendMessage(
                    Component.translatable("lls-manager.command.lbs_blacklist.status").args(
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
                            .suggests(this::getUsernameSuggestions).executes(this).then(
                                    RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString()).executes(this::runWithReason))
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
            source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.add.failure")
                    .color(NamedTextColor.RED)
                    .args(TextUtil.getUsernameComponent(username))
            );
            return 0;
        }



        private int execute(CommandSource source, String username) {
            return execute(source, username, "");
        }

        private int execute(CommandSource source, String username, String reason) {
            @Nullable UUID playerUUID = UUIDUtil.getUUID(llsManager, username);
            if (playerUUID == null) {
                return errorOccurred(source, username);
            }

            if (llsManager.lbsWhiteList.isPlayerBlacklisted(playerUUID)) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.add.already_in_blacklist")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW),
                                Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)));
                return 0;
            } else {
                boolean saved = llsManager.lbsWhiteList.blacklistAdd(playerUUID, reason);
                if (!saved) {
                    return errorOccurred(source, username);
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.add.success")
                            .color(NamedTextColor.GREEN)
                            .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW),
                                    Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)));

                    @Nullable Component banReasonComponent = llsManager.lbsWhiteList.getReasonComponent(reason, null);
                    if (banReasonComponent == null) {
                        source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.reason.not_set"));
                    } else {
                        source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.reason.set").args(banReasonComponent));
                    }
                    return 1;
                }
            }
        }

        public int runWithReason(CommandContext<CommandSource> commandContext) {
            String username = commandContext.getArgument("username/uuid", String.class);
            String reason = commandContext.getArgument("reason", String.class);
            CommandSource source = commandContext.getSource();
            return execute(source, username, reason);
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            String username = commandContext.getArgument("username/uuid", String.class);
            CommandSource source = commandContext.getSource();
            return execute(source, username);
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
            source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.remove.failure")
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

            if (!llsManager.lbsWhiteList.isPlayerBlacklisted(playerUUID)) {
                source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.remove.not_in_blacklist")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW),
                                Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)));
                return 0;
            } else {
                boolean saved = llsManager.lbsWhiteList.blacklistRemove(playerUUID);
                if (!saved) {
                    return errorOccurred(source, username);
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lbs_blacklist.remove.success")
                            .color(NamedTextColor.RED)
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

        private Component notBlacklisted(String target){
            return notBlacklisted(target, NamedTextColor.YELLOW);
        }

        private Component notBlacklisted(String target, NamedTextColor color) {
            return Component.translatable("lls-manager.command.lbs_blacklist.query.not").args(
                    Component.text(target).color(color)
            );
        }

        private Component getPlayerBannedReason(UUID playerUUID) {
            Component reasonComp = llsManager.lbsWhiteList.getPlayerBannedReason(playerUUID, null);
            if (reasonComp == null) {
                return Component.translatable("lls-manager.command.lbs_blacklist.reason.not_set");
            } else {
                return Component.translatable("lls-manager.command.lbs_blacklist.reason.set").args(reasonComp);
            }
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
                if (onlineUUID != null && llsManager.lbsWhiteList.isPlayerBlacklisted(onlineUUID)) {
                    found = true;
                    source.sendMessage(
                            Component.translatable("lls-manager.command.lbs_blacklist.query.username.online_in")
                                    .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW), Component.text(onlineUUID.toString()).color(NamedTextColor.GOLD))
                    );
                    source.sendMessage(getPlayerBannedReason(onlineUUID));
                }
                if (offlineUUID != null && llsManager.lbsWhiteList.isPlayerBlacklisted(offlineUUID)) {
                    found = true;
                    source.sendMessage(
                            Component.translatable("lls-manager.command.lbs_blacklist.query.username.offline_in")
                                    .args(TextUtil.getUsernameComponent(username).color(NamedTextColor.YELLOW), Component.text(offlineUUID.toString()).color(NamedTextColor.GOLD))
                    );
                    source.sendMessage(getPlayerBannedReason(offlineUUID));
                }
                if (!found) {
                    source.sendMessage(notBlacklisted(username));
                }
            } else {
                if (llsManager.lbsWhiteList.isPlayerBlacklisted(playerUUID)) {
                    source.sendMessage(
                            Component.translatable("lls-manager.command.lbs_blacklist.query.uuid.in").args(
                                    Component.text(playerUUID.toString()).color(NamedTextColor.GOLD)
                            )
                    );
                    source.sendMessage(getPlayerBannedReason(playerUUID));
                } else {
                    source.sendMessage(notBlacklisted(playerUUID.toString(), NamedTextColor.GOLD));
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
                source.sendMessage(Component.translatable(String.format("lls-manager.command.lbs_blacklist.hint%d", i)));
            }
            return 1;
        }
    }
}
