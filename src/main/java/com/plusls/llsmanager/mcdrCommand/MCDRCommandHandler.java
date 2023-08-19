package com.plusls.llsmanager.mcdrCommand;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;

@Singleton
public class MCDRCommandHandler {
    @Inject
    LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.commandManager.register(llsManager.injector.getInstance(MCDReforgedCommand.class).createBrigadierCommand());
    }
}
