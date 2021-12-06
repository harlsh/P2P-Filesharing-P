package logging;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogHelper {
    public FileHandler fileHandler;
    private static String peerID;
    public static Logger log = Logger.getLogger(LogHelper.class.getName());

    public static void logAndPrint(String message) {
        log.info(peerID + ": " + message);
        System.out.println(LogFormatter.getFormattedMessage(message));
    }

    public void initializeLogger(String currentPeerID) {
        try {
            fileHandler = new FileHandler("log-peer_" + currentPeerID + ".log");
            fileHandler.setFormatter(new LogFormatter());
            log.addHandler(fileHandler);
            log.setUseParentHandlers(false);
            peerID = currentPeerID;
        } catch (IOException e) {

        }
    }

    public static class LogFormatter extends Formatter {
        public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

        public static String getFormattedMessage(String message) {
            StringBuilder sb = new StringBuilder();
            return sb.append(dateTimeFormatter.format(LocalDateTime.now()) + " PEER " + message + "\n").toString();
        }

        @Override
        public String format(LogRecord record) {
            return getFormattedMessage(record.getMessage());
        }
    }
}
