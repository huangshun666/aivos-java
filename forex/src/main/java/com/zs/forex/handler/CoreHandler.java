package com.zs.forex.handler;


import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 */
public class CoreHandler {

    private static final ThreadFactory NAMEDTHREADFACTORY =
            new ThreadFactoryBuilder().setNameFormat("forex-core-%d").build();

    private static final ThreadPoolExecutor CONTRACTEXECUTOR = new ThreadPoolExecutor(30,
            60,
            5L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), NAMEDTHREADFACTORY,
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private static final LinkedBlockingQueue<Runnable> RUNNABLEQUEUE = new LinkedBlockingQueue<>();

    public static void start() {

        while (true) {

            Runnable thread;
            try {
                thread = RUNNABLEQUEUE.take();
                CONTRACTEXECUTOR.execute(thread);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public static void addTask(Runnable runnable) {
        RUNNABLEQUEUE.add(runnable);
    }
}
