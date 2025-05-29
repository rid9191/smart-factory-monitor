package com.dashtech.smartfactory;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.fazecast.jSerialComm.SerialPort;

public class SerialTest {
    public static void main(String[] args) {
        // List available ports
        System.out.println("Available Serial Ports:");
        SerialPort[] ports = SerialPort.getCommPorts();
        for (int i = 0; i < ports.length; i++) {
            System.out.println(i + ": " + ports[i].getSystemPortName() + " - " + ports[i].getDescriptivePortName());
        }

        if (ports.length == 0) {
            System.out.println("No serial ports found!");
            return;
        }

        // Select port based on argument or default to first port
        int portIndex = 0;
        if (args.length > 0) {
            try {
                portIndex = Integer.parseInt(args[0]);
                if (portIndex < 0 || portIndex >= ports.length) {
                    System.out.println("Invalid port index! Using first port.");
                    portIndex = 0;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid port index! Using first port.");
            }
        }

        SerialPort testPort = ports[portIndex];
        System.out.println("\nTesting with port: " + testPort.getSystemPortName());

        // Configure the port
        testPort.setBaudRate(9600);
        testPort.setNumDataBits(8);
        testPort.setNumStopBits(1);
        testPort.setParity(SerialPort.NO_PARITY);

        // Open the port
        if (testPort.openPort()) {
            System.out.println("Port opened successfully!");

            // Start a reader thread
            new Thread(() -> {
                byte[] readBuffer = new byte[1024];
                while (testPort.isOpen()) {
                    int numRead = testPort.readBytes(readBuffer, readBuffer.length);
                    if (numRead > 0) {
                        String received = new String(readBuffer, 0, numRead, StandardCharsets.UTF_8);
                        System.out.println("Received: " + received);
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();

            // Main thread handles sending data
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nEnter data to send (type 'exit' to quit):");
            
            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }
                
                byte[] writeBuffer = (input + "\n").getBytes(StandardCharsets.UTF_8);
                int bytesWritten = testPort.writeBytes(writeBuffer, writeBuffer.length);
                System.out.println("Sent " + bytesWritten + " bytes");
            }

            // Cleanup
            testPort.closePort();
            System.out.println("Port closed");
        } else {
            System.out.println("Failed to open port!");
        }
    }
} 