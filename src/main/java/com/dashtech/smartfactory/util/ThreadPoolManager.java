package com.dashtech.smartfactory.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dashtech.smartfactory.model.ThreadStats;

public class ThreadPoolManager {
    private static final Logger logger = LogManager.getLogger(ThreadPoolManager.class);
    
    // Virtual thread executor
    private final ExecutorService virtualExecutor;
    
    // Command queue
    private final LinkedBlockingQueue<Runnable> commandQueue;
    private volatile boolean isRunning = true;
    
    // Monitoring
    private final ScheduledExecutorService monitoringExecutor;
    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    private final AtomicInteger totalTasksSubmitted = new AtomicInteger(0);
    
    private static ThreadPoolManager instance;
    
    private ThreadPoolManager() {
        // Create virtual thread executor with unbounded pool
        virtualExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "VirtualThread-" + threadCount.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
        
        // Initialize command queue
        commandQueue = new LinkedBlockingQueue<>();
        
        // Create platform thread for monitoring
        monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Monitoring-Thread");
            t.setDaemon(true);
            return t;
        });
        
        // Start command processor
        startCommandProcessor();
        
        // Start monitoring
        startMonitoring();
        
        logger.info("ThreadPoolManager initialized with virtual threads");
    }
    
    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }
    
    private void startCommandProcessor() {
        Runnable processor = () -> {
            Thread.currentThread().setName("CommandProcessor");
            while (isRunning) {
                try {
                    Runnable command = commandQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (command != null) {
                        submitTask(() -> {
                            try {
                                command.run();
                            } catch (Exception e) {
                                logger.error("Error executing command", e);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in command processor", e);
                }
            }
        };
        
        virtualExecutor.execute(processor);
    }
    
    private void startMonitoring() {
        monitoringExecutor.scheduleAtFixedRate(() -> {
            ThreadStats stats = ThreadStats.create(
                activeTaskCount.get(),
                totalTasksSubmitted.get(),
                commandQueue.size()
            );
            logger.debug("Thread Pool Status: {}", stats);
        }, 1, 5, TimeUnit.MINUTES);
    }
    
    private void submitTask(Runnable task) {
        totalTasksSubmitted.incrementAndGet();
        activeTaskCount.incrementAndGet();
        
        Runnable wrappedTask = () -> {
            try {
                task.run();
            } finally {
                activeTaskCount.decrementAndGet();
            }
        };
        
        virtualExecutor.execute(wrappedTask);
    }
    
    public void submitSerialTask(Runnable task) {
        submitTask(task);
    }
    
    public void submitWebSocketTask(Runnable task) {
        submitTask(task);
    }
    
    public void queueCommand(Runnable command) {
        try {
            commandQueue.put(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while queueing command", e);
        }
    }
    
    public ThreadStats getCurrentStats() {
        return ThreadStats.create(
            activeTaskCount.get(),
            totalTasksSubmitted.get(),
            commandQueue.size()
        );
    }
    
    public void shutdown() {
        isRunning = false;
        
        // Shutdown executors
        virtualExecutor.shutdown();
        monitoringExecutor.shutdown();
        
        try {
            // Wait for tasks to complete
            if (!virtualExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Some tasks did not complete before shutdown");
            }
            monitoringExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while shutting down", e);
        }
        
        ThreadStats finalStats = getCurrentStats();
        logger.info("ThreadPoolManager shutdown completed. {}", finalStats);
    }
} 