/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils;

import factionsplusplus.FactionsPlusPlus;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class Logger {
    @Inject private FactionsPlusPlus factionsPlusPlus;

    /**
     * Log a debug message to the debug log file if the debug flag is enabled.
     *
     * @param message The message to log.
     */
    public void debug(String message) {
        if (this.factionsPlusPlus.isDebugEnabled()) {
            this.log(message, Level.Debug);
        }
    }

    /**
     * Log a message to the info log/debug file.
     *
     * @param message The message to log.
     */
    public void info(String message) {
        this.log(message, Level.Debug, Level.Info);
    }

    /**
     * Log an error to the the error/debug log file.
     * If an Exception is provided, then the strack
     * trace is logged as well.
     *
     * @param message The message to log.
     * @param exception The exception to log.
     */
    public void error(String message, Exception exception) {
        this.log(message, Level.Debug, Level.Error);
        if (exception != null) {
            message = this.stackTraceToString(exception);
            this.log(message, Level.Debug, Level.Error);
        }
    }

    public void error(String message) {
        this.log(message, Level.Debug, Level.Error);
    }

    /**
     * Log level based messages.
     * @param level
     * @param messages
     */
    public void log(String message, Level... levels) {
        this.logToFile(message, levels);
    }

    private void logToFile(String message, Level... levels) {
        DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Level level : levels) {
            String messageFormat = String.format(
                "[%s] [%s] %s",
                level.toString(),
                LocalDateTime.now().format(formatter),
                message
            );

            File path = new File(Path.of(
                this.factionsPlusPlus.getStoragePath(),
                "logs",
                level.toString()
            ).toString());

            if (! path.exists()) {
                path.mkdirs();
            }

            try {
                File file = new File(String.format(
                    "%s/%s.txt",
                    path.getAbsolutePath(),
                    LocalDateTime.now().format(fileNameFormatter))
                );

                if (! file.exists()) {
                    file.createNewFile();
                }
                FileWriter fileWriter = new FileWriter(file, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(messageFormat);
                bufferedWriter.newLine();
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String stackTraceToString(Exception e) {
        try {
            try (
                StringWriter stringWiter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWiter)
            ) {
                e.printStackTrace(printWriter);
                return stringWiter.toString();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return null;
    }

    public enum Level {
        Info("info"),
        Debug("debug"),
        Error("error");

        String logName;

        Level(String logName) {
            this.logName = logName;
        }

        public String toString() {
            return this.logName;
        }
    }
}