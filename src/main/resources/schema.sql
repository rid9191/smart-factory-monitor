-- Drop tables if they exist
DROP TABLE IF EXISTS command_log;
DROP TABLE IF EXISTS serial_data;

-- Create command log table
CREATE TABLE IF NOT EXISTS COMMAND_LOG (
                     ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                     ACTUATOR_ID INT NOT NULL,
                     COMMAND_LOG VARCHAR(50) NOT NULL,
                     SUCCESS BOOLEAN NOT NULL,
                     ERROR_MESSAGE VARCHAR(255),
                     "TIMESTAMP" TIMESTAMP NOT NULL,
                     OPERATOR_ID VARCHAR(50) NOT NULL
                 )

-- Create serial data table
CREATE TABLE serial_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL
);


--
REATE TABLE IF NOT EXISTS SENSOR_DATA (
                ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                SENSOR_ID INT NOT NULL,
                DATA_TYPE VARCHAR(50) NOT NULL,
                "VALUE" DOUBLE NOT NULL,
                TIMESTAMP TIMESTAMP NOT NULL
            )

-- Create indexes
CREATE INDEX idx_command_log_timestamp ON command_log(timestamp);
CREATE INDEX idx_serial_data_timestamp ON serial_data(timestamp); 