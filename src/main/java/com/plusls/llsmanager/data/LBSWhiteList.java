package com.plusls.llsmanager.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


public class LBSWhiteList extends AbstractConfig<LBSWhiteList.WhitelistData> {
    private WhitelistData whitelistData = new WhitelistData();

    public LBSWhiteList(Path dataFolderPath) {
        super(dataFolderPath.resolve("lbs-whitelist.json"), WhitelistData.class);
    }

    public static class WhitelistData {
        public boolean enableWhiteList = false;
        public String whiteListBlockReason = "";
        public ConcurrentSkipListSet<String> whiteList = new ConcurrentSkipListSet<>();
        public boolean enableBlackList = true;
        public ConcurrentHashMap<String, String> blackList = new ConcurrentHashMap<>();
    }

    public boolean isWhiteListEnabled() {
        return whitelistData.enableWhiteList;
    }

    public boolean isBlackListEnabled() {
        return whitelistData.enableBlackList;
    }

    public boolean isPlayerWhitelisted(UUID uuid) {
        return whitelistData.whiteList.contains(uuid.toString());
    }

    public boolean isPlayerBlacklisted(UUID uuid) {
        return whitelistData.blackList.containsKey(uuid.toString());
    }

    public @Nullable Component getReasonComponent(String reason, @Nullable Component defaultReason) {
        if (reason.isEmpty()) {
            return defaultReason;
        }
        try {
            return GsonComponentSerializer.gson().deserialize(reason);
        } catch (Exception e) {
            return Component.text(reason);
        }
    }

    public Component getPlayerBannedReason(UUID uuid) {
        return getReasonComponent(
                Objects.requireNonNull(whitelistData.blackList.get(uuid.toString())),
                Component.translatable("multiplayer.disconnect.banned")
        );
    }

    public @Nullable Component getPlayerBannedReason(UUID uuid, @Nullable Component defaultReason) {
        return getReasonComponent(
                Objects.requireNonNull(whitelistData.blackList.get(uuid.toString())),
                defaultReason
        );
    }

    public Component getWhiteListBlockReason(){
        return getReasonComponent(
                whitelistData.whiteListBlockReason,
                Component.translatable("multiplayer.disconnect.not_whitelisted")
        );
    }

    public boolean whitelistAdd(UUID uuid) {
        if (isPlayerWhitelisted(uuid)) {
            return false;
        } else {
            whitelistData.whiteList.add(uuid.toString());
            return save();
        }
    }

    public boolean blacklistAdd(UUID uuid, Component reason) {
        return blacklistAdd(uuid, GsonComponentSerializer.gson().serialize(reason));
    }

    public boolean blacklistAdd(UUID uuid, String reason) {
        if (isPlayerBlacklisted(uuid)) {
            return false;
        } else {
            whitelistData.blackList.put(uuid.toString(), reason);
            return save();
        }
    }

    public boolean whitelistRemove(UUID uuid) {
        if (!isPlayerWhitelisted(uuid)) {
            return false;
        } else {
            whitelistData.whiteList.remove(uuid.toString());
            return save();
        }
    }

    public boolean blacklistRemove(UUID uuid) {
        if (!isPlayerBlacklisted(uuid)) {
            return false;
        } else {
            whitelistData.blackList.remove(uuid.toString());
            return save();
        }
    }

    public boolean blacklistAdd(UUID uuid) {
        return blacklistAdd(uuid, Component.translatable("multiplayer.disconnect.banned").color(NamedTextColor.RED));
    }

    public boolean setWhitelistStatus(boolean status) {
        if (whitelistData.enableWhiteList == status) {
            return false;
        } else {
            whitelistData.enableWhiteList = status;
            return save();
        }
    }

    public boolean setBlacklistStatus(boolean status) {
        if (whitelistData.enableBlackList == status) {
            return false;
        } else {
            whitelistData.enableBlackList = status;
            return save();
        }
    }

    @Override
    protected WhitelistData getData() {
        return whitelistData;
    }

    @Override
    protected void setData(WhitelistData data) {
        whitelistData = data;
    }
}
