package org.Card2K.scheduler;

import org.Card2K.NapThePlugin;

public interface SchedulerAdapter {
    void startRepeatingTask(Runnable task, long delayTicks);
    void runTaskLater(Runnable task, long delayTicks);
    void runTask(NapThePlugin plugin, Runnable task);
    void cancelAll();
}
