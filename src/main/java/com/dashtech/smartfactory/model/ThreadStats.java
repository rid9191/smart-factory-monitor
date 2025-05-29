package com.dashtech.smartfactory.model;

import java.time.Instant;

/**
 * Record representing thread pool statistics at a point in time.
 * @param activeTasks Number of currently active tasks
 * @param totalTasksSubmitted Total number of tasks submitted since startup
 * @param queueSize Current size of the command queue
 * @param timestamp Time when these statistics were captured
 */
public record ThreadStats(
    int activeTasks,
    int totalTasksSubmitted,
    int queueSize,
    Instant timestamp
) {
    /**
     * Factory method to create a new ThreadStats instance with current timestamp
     */
    public static ThreadStats create(int activeTasks, int totalTasksSubmitted, int queueSize) {
        return new ThreadStats(activeTasks, totalTasksSubmitted, queueSize, Instant.now());
    }
    
    /**
     * Returns a formatted string representation of the statistics
     */
    @Override
    public String toString() {
        return String.format(
            "Thread Stats [Active: %d, Total: %d, Queued: %d, Time: %s]",
            activeTasks, totalTasksSubmitted, queueSize, timestamp
        );
    }
} 