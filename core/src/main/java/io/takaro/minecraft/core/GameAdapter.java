package io.takaro.minecraft.core;

public interface GameAdapter {
    void logInfo(String msg);
    void logWarning(String msg);
    void runOnMainThread(Runnable task);
}
