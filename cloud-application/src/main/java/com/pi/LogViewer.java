// filepath: /home/iot_ubuntu_intel/contiki-ng/examples/project/cloud-application/src/main/java/com/pi/LogViewer.java
package com.pi;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LogViewer {
    private static final String LOG_FILE = "logs/coap-application.log";
    private static final int MAX_LINES_TO_SHOW = 50;

    public LogViewer() {
        // Create logs directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get("logs"));
        } catch (IOException e) {
            System.err.println("Failed to create logs directory: " + e.getMessage());
        }
    }

    public void showRecentLogs() {
        System.out.println("\n=== RECENT LOGS ===");
        showLastNLines(MAX_LINES_TO_SHOW);
    }

    public void showLastNLines(int n) {
        System.out.println("\n=== LAST " + n + " LOG LINES ===");
        try {
            List<String> lines = Files.readAllLines(Paths.get(LOG_FILE));
            int start = Math.max(0, lines.size() - n);

            for (int i = start; i < lines.size(); i++) {
                System.out.println(lines.get(i));
            }
        } catch (IOException e) {
            System.out.println("No logs available yet or error reading log file: " + e.getMessage());
        }
        System.out.println("=== END LOGS ===\n");
    }
}