package com.dashtech.smartfactory.model;

import java.nio.file.Path;

/**
 * Record representing database configuration with immutable properties.
 */
public record DatabaseConfig(
    String url,
    String username,
    String password,
    int maxPoolSize,
    int minPoolSize
) {
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_POOL_SIZE = 2;
    
    /**
     * Creates a default H2 database configuration with the given database path
     */
    public static DatabaseConfig createDefault(Path dbPath) {
        /*
         * DB_CLOSE_DELAY=-1        : Keeps DB open after last connection; requires explicit SHUTDOWN
         * DB_CLOSE_ON_EXIT=FALSE   : Prevents H2 from auto-closing DB via shutdown hooks
         * WRITE_DELAY=0            : Ensures all writes are immediate (disable writer thread)
         * TRACE_LEVEL_FILE=0       : Minimizes shutdown logging in file
         * MODE=MySQL               : Use MySQL compatibility mode for better identifier handling
         */
        String url = String.format(
            "jdbc:h2:file:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0;TRACE_LEVEL_FILE=0;MODE=MySQL",
            dbPath.toAbsolutePath().toString()
        );
        
        return new DatabaseConfig(
            url,
            "sa",
            "",
            DEFAULT_MAX_POOL_SIZE,
            DEFAULT_MIN_POOL_SIZE
        );
    }
    
    /**
     * Creates an in-memory H2 database configuration for testing
     */
    public static DatabaseConfig createInMemory() {
        return new DatabaseConfig("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "", 5, 1);
    }
} 