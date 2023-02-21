package com.plusls.llsmanager.tabListSync;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

public class TabListUtil {
    public static TabListEntry getTabListEntry(TabList tabList, Player player) {
        return TabListEntry.builder()
                .tabList(tabList)
                .profile(player.getGameProfile())
                .displayName(getRegularDisplayName(player))
                .latency(Long.valueOf(player.getPing()).intValue())
                .gameMode(0)
                .build();
    }

    public static Component getServerDisplayName(Player player, ServerConnection currentServer) {
        return TextUtil.getUsernameComponentWithoutEvent(player.getGameProfile().getName())
                .append(Component.text(" ["))
                .append(TextUtil.getServerNameComponent(currentServer.getServerInfo().getName()))
                .append(Component.text(']'));
    }

    public static Component getRegularDisplayName(Player player) {
        return Component.text(player.getGameProfile().getName());
    }

    public static void updateTabListEntry(TabListEntry tabListEntry, Player player, Player itemPlayer) {
        Component component = null;
        if (tabListEntry.getDisplayNameComponent().isPresent()) {
            component = tabListEntry.getDisplayNameComponent().get();
        }

        if (inSameServer(player, itemPlayer)) {
            if (!Objects.equals(component, getRegularDisplayName(itemPlayer))) {
                tabListEntry.setDisplayName(getRegularDisplayName(itemPlayer));
            }
        } else {
            itemPlayer.getCurrentServer().ifPresent(
                    serverConnection -> tabListEntry.setDisplayName(getServerDisplayName(itemPlayer, serverConnection))
            );
        }

        int latency = Long.valueOf(itemPlayer.getPing()).intValue();
        if (tabListEntry.getLatency() != latency) {
            tabListEntry.setLatency(latency);
        }

        if (!inSameServer(player, itemPlayer)) {
            if (tabListEntry.getGameMode() != 0) {
                tabListEntry.setGameMode(0);
            }
        }
    }

    private static boolean inSameServer(Player player1, Player player2) {
        if (player1.getCurrentServer().isEmpty()) {
            return false;
        }
        String server1 = player1.getCurrentServer().get().getServerInfo().getName();
        if (player2.getCurrentServer().isEmpty()) {
            return false;
        }
        String server2 = player2.getCurrentServer().get().getServerInfo().getName();
        return server1.equals(server2);
    }

    // 已知，若是服务器存在同名玩家不使用 velocity 登陆会出现 bug，比如存在多个代理
    // 或者 carpet 假人使用了 shadow
    // TODO 鉴权，未登录玩家不能看到全服列表
    // Yuki note: execute from plugin event instead of packet 2023.2.18
    public static void updateTabList(LlsManager llsManager) {

        // synchronized (llsManager.injector.getInstance(TabListSyncHandler.class).currentItems) {
            /* for (LegacyPlayerListItem.Item item : playerListItem.getItems()) {
                String name = null;
                if (!item.getName().equals("")) {
                    name = item.getName();
                } else {
                    for (Map.Entry<String, LegacyPlayerListItem.Item> entry : currentItems.entrySet()) {
                        if (Objects.equals(item.getUuid(), entry.getValue().getUuid())) {
                            name = entry.getKey();
                            break;
                        }
                    }
                }
                // 只处理在服务器中的玩家
                if (name == null || llsManager.server.getPlayer(name).isEmpty()) {
                    continue;
                }
                switch (playerListItem.getAction()) {
                    case LegacyPlayerListItem.ADD_PLAYER:
                        currentItems.put(name, item);
                        break;
                    case LegacyPlayerListItem.UPDATE_GAMEMODE:
                        Objects.requireNonNull(currentItems.get(name)).setGameMode(item.getGameMode());
                        break;
                    case LegacyPlayerListItem.UPDATE_LATENCY:
                        Objects.requireNonNull(currentItems.get(name)).setLatency(item.getLatency());
                        break;
                    case LegacyPlayerListItem.UPDATE_DISPLAY_NAME:
                        Objects.requireNonNull(currentItems.get(name)).setDisplayName(item.getDisplayName());
                        break;
                    case LegacyPlayerListItem.REMOVE_PLAYER:
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown action " + playerListItem.getAction());
                }
            } */

        for (Player toPlayer : llsManager.server.getAllPlayers()) {
            TabList tabList = toPlayer.getTabList();
            // 添加缺失的  tabListEntry
            for (Player entryPlayer : llsManager.server.getAllPlayers()) {
                // tabList 不处理自己
                if (!entryPlayer.getGameProfile().getName().equals(toPlayer.getGameProfile().getName())) {

                    boolean shouldAdd = true;
                    for (TabListEntry tabListEntry : tabList.getEntries()) {
                        if (tabListEntry.getProfile().getName().equals(entryPlayer.getGameProfile().getName())) {
                            shouldAdd = false;
                            break;
                        }
                    }
                    if (shouldAdd) {
                        tabList.addEntry(TabListUtil.getTabListEntry(tabList, entryPlayer));
                    }
                }
            }
            // 更新 tabListEntry
            for (TabListEntry tabListEntry : tabList.getEntries()) {
                llsManager.server.getPlayer(tabListEntry.getProfile().getName()).ifPresentOrElse(
                        itemPlayer -> {
                            updateTabListEntry(tabListEntry, toPlayer, itemPlayer);
                        },
                        () -> tabList.removeEntry(tabListEntry.getProfile().getId())
                );
            }
        }
        //}
    }
}