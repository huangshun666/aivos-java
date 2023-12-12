package com.zs.self.task;

import jodd.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author IsZssTao
 */
public class CoreHandler {

    private static final ThreadFactory NAMEDTHREADFACTORY =
            new ThreadFactoryBuilder().setNameFormat("handler-pool-%d").get();

    private static final ThreadPoolExecutor CONTRACTEXECUTOR = new ThreadPoolExecutor(40,
            60,
            5L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), NAMEDTHREADFACTORY,
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private static final LinkedBlockingQueue<Runnable> RUNNABLEQUEUE = new LinkedBlockingQueue<>();

    public static void start() throws InterruptedException {

        while (true) {
            Runnable thread = RUNNABLEQUEUE.take();
            CONTRACTEXECUTOR.execute(thread);
        }
    }

    public static void addTask(Runnable runnable) {
        RUNNABLEQUEUE.add(runnable);
    }
}
