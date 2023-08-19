package com.plusls.llsmanager.whitelist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.util.GameProfile;

import java.util.UUID;

@Singleton
public class WhitelistHandler {
    @Inject
    public LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(WhitelistHandler.class));
        if (llsManager.config.getWhitelist()) {
            llsManager.commandManager.register(llsManager.injector.getInstance(LlsWhitelistCommand.class).createBrigadierCommand());
            llsManager.commandManager.register(llsManager.injector.getInstance(LlsBlacklistCommand.class).createBrigadierCommand());
        }
    }

    @Subscribe(order =  PostOrder.EARLY)
    public void onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }
        if (!llsManager.config.getWhitelist()) {
            return;
        }
        GameProfile profile = event.getPlayer().getGameProfile();
        UUID uuid = profile.getId();
        if (llsManager.lbsWhiteList.isBlackListEnabled() && llsManager.lbsWhiteList.isPlayerBlacklisted(uuid)) {
            event.setResult(ResultedEvent.ComponentResult.denied(llsManager.lbsWhiteList.getPlayerBannedReason(uuid)));
            return;
        }
        if (llsManager.lbsWhiteList.isWhiteListEnabled() && !llsManager.lbsWhiteList.isPlayerWhitelisted(uuid)) {
            event.setResult(ResultedEvent.ComponentResult.denied(llsManager.lbsWhiteList.getWhiteListBlockReason()));
        }
    }

/*    @Subscribe(order = PostOrder.EARLY)
    public void onPreLogin(PreLoginEvent event) {
        // 如果别的插件阻止了，那就不查了需要做检
        if (!event.getResult().isAllowed()) {
            return;
        }
        // 未开启白名单则不需要检查
        if (!llsManager.config.getWhitelist()) {
            return;
        }
        String username = ConnectionUtil.getUsername(llsManager, event.getUsername(), event.getConnection());

        // 如果没有这个用户，则说明它不在白名内，直接断开连接
        LlsPlayer llsPlayer;
        try {
            llsPlayer = llsManager.getLlsPlayer(username);
        } catch (LoadPlayerFailException | PlayerNotFoundException e) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED)));
            return;
        }
        // 如果白名服务器列表为空也断开连接
        // 如果是离线认证玩家则还需要保证 auth server 在白名单内
        if (llsPlayer.getWhitelistServerList().isEmpty() || (!llsPlayer.getOnlineMode() && !checkServer(llsPlayer, llsManager.config.getAuthServerName()))) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED)));
        }

    }

/*    private boolean checkServer(LlsPlayer llsPlayer, String serverName) {
        Set<String> whitelistServerList = llsPlayer.getWhitelistServerList();
        if (whitelistServerList.contains(serverName)) {
            return true;
        }
        for (String whitelistServerName : whitelistServerList) {
            Set<String> serverGroup = llsManager.config.getServerGroup().get(whitelistServerName);
            if (serverGroup != null && serverGroup.contains(serverName)) {
                return true;
            }
        }
        return false;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        // 如果别的插件阻止了，那就不需要做检查了
        if (event.getResult().getServer().isEmpty()) {
            return;
        }
        // 未开启白名单则不需要检查
        if (!llsManager.config.getWhitelist()) {
            return;
        }
        String serverName = event.getResult().getServer().get().getServerInfo().getName();

        Player player = event.getPlayer();

        LlsPlayer llsPlayer = llsManager.getLlsPlayer(player);

        if (!checkServer(llsPlayer, serverName)) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED));
            if (llsPlayer.getWhitelistServerList().isEmpty() || (!llsPlayer.getOnlineMode() && !llsPlayer.getWhitelistServerList().contains(llsManager.config.getAuthServerName()))) {
                player.disconnect(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED));
            }
        }
    }
*/
}
