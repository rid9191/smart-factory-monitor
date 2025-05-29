# Smart Factory Monitor

A real-time industrial process monitoring system built with Java and WebSocket technology. This application provides a robust interface for monitoring sensor data and controlling industrial actuators through a web-based dashboard.

## Features

- Real-time sensor data monitoring (Temperature, Pressure, Humidity)
- Bi-directional serial communication with industrial equipment
- WebSocket-based real-time updates
- Historical data visualization
- Actuator control interface
- Embedded H2 database for data persistence
- Comprehensive logging system
- Responsive web dashboard with Chart.js
- Error handling and recovery mechanisms

## Prerequisites

- Java 17 or newer
- Apache Maven 3.8+
- Apache Tomcat 10.x
- Modern web browser with WebSocket support

## Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/smartfactory.git
   cd smartfactory
   ```

2. Build with Maven:
   ```bash
   mvn clean package
   ```

   This will create a WAR file in the `target` directory.

## Deployment

1. Copy the WAR file to Tomcat's webapps directory:
   ```bash
   cp target/smartfactory.war $TOMCAT_HOME/webapps/
   ```

2. Configure system properties in Tomcat's `setenv.sh` (Linux) or `setenv.bat` (Windows):
   ```bash
   # Linux
   export JAVA_OPTS="$JAVA_OPTS \
       -Dsmartfactory.db.path=/path/to/data/smartfactory \
       -Dsmartfactory.serial.port=COM1 \
       -Dsmartfactory.serial.baudrate=9600"

   # Windows
   set JAVA_OPTS=%JAVA_OPTS% ^
       -Dsmartfactory.db.path=C:/path/to/data/smartfactory ^
       -Dsmartfactory.serial.port=COM1 ^
       -Dsmartfactory.serial.baudrate=9600
   ```

3. Start Tomcat:
   ```bash
   $TOMCAT_HOME/bin/startup.sh   # Linux
   %TOMCAT_HOME%\bin\startup.bat # Windows
   ```

4. Access the application at:
   ```
   http://localhost:8080/smartfactory/
   ```

## Configuration

### Database Configuration
- Default location: `./data/smartfactory`
- Configure using system property: `smartfactory.db.path`

### Serial Communication
- Default port: COM1
- Default baud rate: 9600
- Configure using system properties:
  - `smartfactory.serial.port`
  - `smartfactory.serial.baudrate`

### Logging
- Log files location: `./logs/`
- Main log file: `smartfactory.log`
- Error log file: `error.log`
- Configure using `src/main/resources/log4j2.xml`

## Serial Protocol

### Sensor Data Packet Structure
```
[Header: 2 bytes] [Sensor ID: 1 byte] [Data Type: 1 byte] [Payload: 4 bytes] [Checksum: 1 byte]
- Header: 0xAABB
- Data Types:
  - 0x01: Temperature
  - 0x02: Pressure
  - 0x03: Humidity
```

### Control Command Packet Structure
```
[Header: 2 bytes] [Actuator ID: 1 byte] [Command: 1 byte] [Payload: 4 bytes] [Checksum: 1 byte]
- Header: 0xCCDD
- Commands:
  - 0x00: OFF
  - 0x01: ON
```

## Development

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── dashtech/
│   │           └── smartfactory/
│   │               ├── model/
│   │               ├── service/
│   │               ├── util/
│   │               └── websocket/
│   ├── resources/
│   │   └── log4j2.xml
│   └── webapp/
│       ├── WEB-INF/
│       │   └── web.xml
│       ├── error/
│       └── index.html
```

### Key Components
- `SmartFactoryApplication`: Main application context listener
- `SerialCommunicationService`: Handles serial port communication
- `DatabaseService`: Manages data persistence
- `SmartFactoryWebSocket`: WebSocket endpoint for real-time communication

## Testing

Run the test suite:
```bash
mvn test
```

## Simulating Serial Communication

For development and testing without actual hardware:

1. Use a virtual serial port pair:
   - Windows: Use com0com
   - Linux: Use socat
   ```bash
   socat -d -d pty,raw,echo=0 pty,raw,echo=0
   ```

2. Use the test utility to simulate sensor data:
   ```bash
   java -cp target/smartfactory.jar com.dashtech.smartfactory.util.SerialSimulator
   ```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, please open an issue in the GitHub repository or contact support@example.com.