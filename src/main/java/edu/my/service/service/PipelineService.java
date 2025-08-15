package edu.my.service.service;

import edu.my.service.inter.EnvelopeSender;
import edu.my.service.model.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static net.logstash.logback.argument.StructuredArguments.kv;

public class PipelineService {
    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);
    private final ThreadPoolExecutor exec;
    private final CompletionService<Result> ecs;
    private final FileProcessor processor;
    private final EnvelopeSender sender;
    private final Path processingDir, doneDir, errorDir;

    public PipelineService(FileProcessor processor, EnvelopeSender sender, int workers, Path processingDir, Path doneDir, Path errorDir) throws IOException {
        this.processor = processor;
        this.sender = sender;
        this.processingDir = processingDir;
        this.doneDir = doneDir;
        this.errorDir = errorDir;

        Files.createDirectories(processingDir);
        Files.createDirectories(doneDir);
        Files.createDirectories(errorDir);

        this.exec = new ThreadPoolExecutor(
                workers, workers, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),//очередь при переполнении пула
                r -> { var t = new Thread(r, "zip-worker"); t.setDaemon(false); return t; },
                new ThreadPoolExecutor.CallerRunsPolicy());//если все забито - выполняем в вызывающем потоке - backpressure
        this.ecs = new java.util.concurrent.ExecutorCompletionService<>(exec);
    }

    /**
     * Нашёл файл в INBOX, закинул в мясорубку.
     * @param inboxZip путь к архиву (zip, rar сюда не пихать — не взлетит)
     * @throws IOException если Files.move решил вспомнить о своём существовании
     *
     * Примечание:
     * Этот метод — триггер для всех, кто когда-то говорил, что:
     *   1) try-catch — это для слабаков
     *   2) Java — мёртвая, медленная и вообще зачем она
     *   3) "В Go было бы проще"
     *
     * Если ты из этих — знай, я рад, что ты сейчас читаешь это и кипятишься.
     * И да, тут нет твоего любимого panic(err).
     */
    public void submitWithMdc(Path inboxZip, Map<String,String> mdc) throws Exception {
        // move -> processing, как у тебя
        Path locked = processingDir.resolve(inboxZip.getFileName());
        Files.move(inboxZip, locked, StandardCopyOption.ATOMIC_MOVE);

        ecs.submit(() -> {
            Map<String,String> old = MDC.getCopyOfContextMap();
            if (mdc != null) MDC.setContextMap(mdc); else MDC.clear();
            long t0 = System.nanoTime();
            try {
                // processZip -> send -> move to done/error
                log.info("processing started", kv("locked", locked.toString()));
                Optional<Envelope> opt = processor.processZipFile(locked);//распаковываем зип, парсим и делаем конверт
                opt.ifPresent(e -> {
                    MDC.put("envType", e.type().name());
                    MDC.put("branchId", e.branchId());
                });

                long t = System.nanoTime() - t0;
                log.info("processing finished", kv("durationMs", t / 1_000_000));
                Map<String,String> mdcForDispatch = MDC.getCopyOfContextMap();
                return new Result(locked, opt, mdcForDispatch);
            } finally {
                if (old != null) MDC.setContextMap(old); else MDC.clear();
            }
        });
    }

    /**
     * Смысл всего метода: это «вторая половина конвейера» — ждать готовые результаты, отправлять их,
     * и раскладывать zip-файлы по done/ и error/.
     * Благодаря CompletionService мы обрабатываем задачи по мере готовности, а не по порядку.
     *
     * Примечание:
     * Конструкции кроме if-else ты видишь впервые. Они тебе могут быть не понятны. И я очень этому рад
     * Жидкость для охлаждения ануса, увы, в репозиторий не вошла, но я верю, ты справишься
     */
    public void startDispatcherLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    Future<Result> f = ecs.take();//ждем пока кто-то в пуле кончит (CompletionService — бог out-of-order обработки)
                    Result r = f.get();//либо результат, либо Exception
                    Map<String,String> prev = MDC.getCopyOfContextMap();
                    if (r.mdc != null) MDC.setContextMap(r.mdc); else MDC.clear();
                    if (r.envelope.isPresent()) {
                        try {
                            sender.send(r.envelope.get());//отправка в Quarkus. Бросает Exception, если 4хх|5хх
                            Files.move(r.lockedZip, doneDir.resolve(r.lockedZip.getFileName()),//переносим в папку done
                                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        } catch (Exception sendErr) {
                            log.error("send failed", sendErr);//отправка наебнулась
                            Files.move(r.lockedZip, errorDir.resolve(r.lockedZip.getFileName()),//переносим в error-папку
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    } else {
                        //распарсили, распаковали, но конверта нет - сразу в error
                        log.warn("send failed - no payload");
                        Files.move(r.lockedZip, errorDir.resolve(r.lockedZip.getFileName()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                    /*
                      Поручик Ржевский собирается на бал. Его денщик видит, как тот одеколоном смазывает конец.
                      — Это зачем, вашебродь?
                      — Да так, Иван, на всякий случай.
                      Через некоторое время поручик начинает смазывать одеколоном задницу.
                      — А это зачем, вашебродь?
                      — Да случаи — они разные бывают...
                     */
                } catch (IOException e) {
                    Throwable cause = e.getCause();
                    log.error("file move failed. Caused by", cause);
                    log.error("file move failed", e);
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    log.error("worker task failed. Caused by", cause);
                    log.error("worker task failed", ee);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Throwable cause = ie.getCause();
                    log.warn("dispatch loop interrupted. Caused by ", cause);
                    log.warn("dispatch loop interrupted: ", ie);
                    return;
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    log.warn("dispatch failed. Caused by ", cause);
                    log.warn("dispatch failed: ", e);
                }
            }
        }, "dispatch-loop").start();
    }

    public void shutdown() {
        log.info("shutting down pipeline");
        exec.shutdown();
        try {
            if (!exec.awaitTermination(30, TimeUnit.SECONDS)) exec.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exec.shutdownNow();
        }
    }


    /**
     * маленький immutable DTO:
     * @param lockedZip - куда мы переместили исходный архив (в processing/);
     * @param envelope - результат парсинга (может отсутствовать).
     */
    record Result(
            Path lockedZip,
            Optional<Envelope> envelope,
            Map<String,String> mdc) {}
}

