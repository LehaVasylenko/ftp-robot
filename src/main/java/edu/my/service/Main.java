package edu.my.service;

import edu.my.service.model.EnvelopeType;
import edu.my.service.service.*;

import java.net.URI;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final Path INBOX = Paths.get("src/main/java/edu/my/service/ftp/inbox");
    private static final Path PROCESSING = Paths.get("src/main/java/edu/my/service/ftp/processing");
    private static final Path DONE = Paths.get("src/main/java/edu/my/service/ftp/done");
    private static final Path ERROR = Paths.get("src/main/java/edu/my/service/ftp/error");

    public static void main(String[] args) throws Exception {
        log.info("Starting up...");
        var router = new EnvelopeSenderRouter(Map.of(
                EnvelopeType.GOODS, new HttpSenderService(URI.create("http://localhost:8082/mapper/v1/api/robot/nom")),
                EnvelopeType.REST,  new HttpSenderService(URI.create("http://localhost:8082/mapper/v1/api/robot/rests"))
        ));
        log.info("Router ready");

        var processor = new FileProcessor();
        int workers = Math.max(2, Runtime.getRuntime().availableProcessors());
        log.info("Pipeline ready");

        var pipeline = new PipelineService(processor, router, workers, PROCESSING, DONE, ERROR);
        pipeline.startDispatcherLoop();
        log.info("Starting up pipeline... done");

        final InboxWatcher watcher = new InboxWatcher(INBOX, pipeline);

        // graceful shutdown: сперва watcher, затем pipeline
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { watcher.close(); } catch (Exception ignored) {}
            pipeline.shutdown();
        }, "shutdown-hook"));

        log.info("Watching started...");
        try {
            watcher.loop();
        } catch (ClosedWatchServiceException e) {
            log.info("Watcher closed");
        } finally {
            // на случай, если вышли не через hook
            try { watcher.close(); } catch (Exception ignored) {}
            pipeline.shutdown();
        }
    }
}