package com.exetacticals.common;

import net.minecraftforge.event.server.ServerStartedEvent;

public class Commons {

    private static final ThreadLocal<Long> SERVER_START_TIME = ThreadLocal.withInitial(() -> 0L);

    public static void onServerStarted(ServerStartedEvent event) {
        SERVER_START_TIME.set(System.currentTimeMillis());
    }

    public static long getServerStartTime() {
        return SERVER_START_TIME.get();
    }

    public static boolean isServerThread() {
        return SERVER_START_TIME.get() != 0L;
    }

    public static long getServerUptime() {
        long start = SERVER_START_TIME.get();
        return start == 0L ? 0L : (System.currentTimeMillis() - start);
    }
}