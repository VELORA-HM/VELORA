package velora.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + "/velora_errors.log";

    public static void logError(String context, Exception e) {
        File directory = new File(LOG_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = dtf.format(LocalDateTime.now());

        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.printf("[%s] [ERROR] Context: %s | Exception: %s | Message: %s\n",
                    timestamp, context, e.getClass().getSimpleName(), e.getMessage());
        } catch (IOException ioException) {
            System.err.println("Failed to write to log file: " + ioException.getMessage());
        }
    }
}