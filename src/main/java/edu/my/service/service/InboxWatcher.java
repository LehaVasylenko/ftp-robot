package edu.my.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * InboxWatcher — обёртка над WatchService: следит за появлением новых файлов в inboxDir и кидает их в конвейер (pipeline).
 * AutoCloseable — чтобы можно было закрыть вотчер в try-with-resources или при shutdown.
 */
public final class InboxWatcher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(InboxWatcher.class);

    private final Path inboxDir;
    private final WatchService ws;
    private final PipelineService pipeline;

    public InboxWatcher(Path inboxDir, PipelineService pipeline) throws IOException {
        this.inboxDir = inboxDir;
        this.pipeline = pipeline;
        this.ws = FileSystems.getDefault().newWatchService();
        Files.createDirectories(this.inboxDir);
        //подписываемся на событие ENTRY_CREATE — уведомление, когда в каталоге появился новый элемент (файл/папка).
        inboxDir.register(ws, StandardWatchEventKinds.ENTRY_CREATE);
    }

    public void loop() throws InterruptedException {
        // первичный догон
        catchUpExisting();
        while (true) {//вечный цикл (пока не прервут снаружи).
            WatchKey key = ws.take(); // блокируемся
            for (WatchEvent<?> ev : key.pollEvents()) {//take() блокируется, пока не появится пачка событий.
                var kind = ev.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    log.warn("watch overflow; rescanning {}", kv("dir", inboxDir.toString()));
                    catchUpExisting(); // потеряли события — сканируем каталог
                    continue;
                }
                if (kind != StandardWatchEventKinds.ENTRY_CREATE) continue;//нас интересуют только ENTRY_CREATE.
                Path rel = (Path) ev.context(); //относительный путь внутри наблюдаемой папки
                Path created = inboxDir.resolve(rel);//резолвим его в абсолютный
                if (!created.toString().toLowerCase().endsWith(".zip")) continue;//все шо не зип - иди лесом

                // ждём стабилизации и передаём в пайплайн
                if (waitUntilStable(created, 5, 300)) {
                    String reqId = buildReqId(created);
                    MDC.put("reqId", reqId);
                    MDC.put("file", created.getFileName().toString());
                    try {
                        pipeline.submitWithMdc(created, MDC.getCopyOfContextMap());
                        log.info("catch-up submit {}", created.getFileName().toString());
                    } catch (Exception e) {
                        log.error("catch-up failed", e);

                    } finally {
                        MDC.clear();
                    }
                } else {
                    System.err.println("⚠️ Файл не стабилизировался: " + created);
                }
            }
            //Подтверждаем обработку пачки событий. Если вернуть false (или не вызвать) — вотчер перестанет слать события.
            key.reset();
        }
    }

    /**
     * Если в папке есть файлы на момент запуска
     */
    /** Если в папке есть файлы на момент старта */
    private void catchUpExisting() {
        try (var s = Files.list(inboxDir)) {
            s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".zip") && !n.endsWith(".part.zip") && !n.endsWith(".tmp.zip");
                    })
                    .forEach(p -> {
                        if (waitUntilStable(p, 4, 250)) {
                            String reqId = buildReqId(p);
                            MDC.put("reqId", reqId);
                            MDC.put("file", p.getFileName().toString());
                            try {
                                pipeline.submitWithMdc(p, MDC.getCopyOfContextMap());
                                log.info("catch-up submit {}", kv("file", p.getFileName().toString()));
                            } catch (Exception e) {
                                log.error("catch-up failed", e);
                            } finally {
                                MDC.clear();
                            }
                        }
                    });
        } catch (IOException e) {
            log.error("catch-up scan error", e);
        }
    }

    /**
     * Простой анти-race при докачке/дозаписи: 5 раз по 300 мс проверяем, что размер не меняется → считаем файл готов.
     * @param file - имя файла
     * @param checks - кол-во проверок
     * @param sleepMs - тайм-аут между проверками
     * @return - истина - если збсь, ложь - если нет
     */
    private boolean waitUntilStable(Path file, int checks, long sleepMs) {
        try {
            long prevSize = -1, prevTs = -1;
            for (int i = 0; i < checks; i++) {
                long size = Files.size(file);
                long ts = Files.getLastModifiedTime(file).toMillis();
                if (size == prevSize && ts == prevTs) return true;
                prevSize = size; prevTs = ts;
                log.warn("file not stable yet {}", kv("file", file.toString()));
                Thread.sleep(sleepMs);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String buildReqId(Path file) {
        // стабильный id: имя + mtime + размер (если что — UUID)
        try {
            long size = Files.size(file);
            long ts = Files.getLastModifiedTime(file).toMillis();
            return file.getFileName() + "-" + ts + "-" + size;
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Освобождаем системные ресурсы;
     * take() в другом потоке получит ClosedWatchServiceException
     * @throws IOException - почитай доку че close() метод его бросает
     */
    @Override
    public void close() throws IOException {
        ws.close();
    }
}
