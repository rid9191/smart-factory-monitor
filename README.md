# Smart Factory Monitor

A real-time industrial process monitoring & control interface built with Java 17, WebSockets, and bi-directional serial communication. The application streams live sensor data from shop-floor equipment, persists it in an embedded database, and exposes an operator dashboard that can also push control commands back to the factory line.

## Features

- Real-time sensor data monitoring 
- Bi-directional serial communication
- WebSocket-based real-time updates
- Historical data visualization
- Actuator control interface
- Embedded H2 database for data persistence
- Comprehensive logging system with Log4j2
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
   Copy-Item "target/smartfactory.war" -Destination "$TOMCAT_HOME/webapps/ROOT.war" -Force
   ```

2. Configure system properties in Tomcat's `setenv.sh` (Linux) or `setenv.bat` (Windows):
   ```bash
   # Windows
   set JAVA_OPTS=%JAVA_OPTS% ^
       -Dsmartfactory.db.path=C:/path/to/data/smartfactory ^
       -Dsmartfactory.serial.port=COM1 ^
       -Dsmartfactory.serial.baudrate=9600
   ```

3. Start Tomcat:
   ```bash
   %TOMCAT_HOME%\bin\startup.bat # Windows
   ```

4. Access the application at:
   ```
   http://localhost:8080/
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

Testing & Simulation

Create a virtual serial pair using com0com:

install PortName=COM10 PortName=COM11

Launch the simulator on COM11:

java -cp target/smart-factory-monitor.jar com.dashtech.sfm.util.SerialSimulator --port COM11

In the web dashboard:

Select COM10 from the dropdown

Set baud rate to 9600

Click Connect

You should now see real-time sensor data and be able to send actuator commands.

Installing Virtual COM Ports (Windows)

If you see errors like Port COM5 is already used by another device, it means the port is taken by the system (e.g., Bluetooth).

Use safe COM port numbers (e.g., COM10/COM11 or higher):

Open the com0com Setup Command Prompt as Administrator

Run:

install PortName=COM10 PortName=COM11

Confirm availability:

mode COM10
mode COM11

Use COM10 in your app and COM11 in the simulator.

Avoid low-numbered ports like COM1–COM6 which are often reserved or in use.

## Simulating Serial Communication

For development and testing without actual hardware:

1. Use a virtual serial port pair:
   - Windows: Use com0com
   ```bash

2. Use the test utility to simulate sensor data:
   ```bash
   java -cp target/smartfactory.jar com.dashtech.smartfactory.util.SerialSimulator
   

## Support

For support, please open an issue in the GitHub repository or contact support@example.com.