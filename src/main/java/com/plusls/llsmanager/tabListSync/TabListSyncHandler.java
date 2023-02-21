package com.plusls.llsmanager.tabListSync;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
// import com.plusls.llsmanager.util.PacketUtil;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class TabListSyncHandler {
    @Inject
    public LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(TabListSyncHandler.class));
        llsManager.server.getScheduler().buildTask(llsManager, llsManager.injector.getInstance(TabListSyncHandler.class)::updateTabList)
                .repeat(50L, TimeUnit.MILLISECONDS).schedule();
    }

    public void updateTabList() {
        TabListUtil.updateTabList(llsManager);
    }
}