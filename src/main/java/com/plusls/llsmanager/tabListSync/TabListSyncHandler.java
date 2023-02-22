package com.plusls.llsmanager.tabListSync;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;

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