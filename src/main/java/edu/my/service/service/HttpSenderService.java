package edu.my.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.my.service.inter.EnvelopeSender;
import edu.my.service.model.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class HttpSenderService implements EnvelopeSender {
    private static final Logger log = LoggerFactory.getLogger(HttpSenderService.class);
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final int maxAttempts;
    private final Duration requestTimeout;
    private final URI endpoint;

    public HttpSenderService(URI endpoint) {
        this.endpoint = endpoint;
        this.maxAttempts = 1;
        this.requestTimeout = Duration.ofSeconds(15);

        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.mapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void send(Envelope e) throws Exception {
        byte[] body = serialize(e);
        String idempotencyKey = buildIdempotencyKey(e);

        HttpRequest req = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", "my-service/1.0")
                .header("X-Request-ID", idempotencyKey)
                .header("Authorization", "Basic Q3IwYzBkaTExby1CMG1iYXJkaTExMDpDYXB1Y2gxbm4wLUFzc2FzMW4w")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        var prevMdc = org.slf4j.MDC.getCopyOfContextMap();
        MDC.put("idemKey", idempotencyKey);
        MDC.put("endpoint", endpoint.toString());
        long t0 = System.nanoTime();

        // Ретраи с экспоненциальным бэкоффом
        int attempt = 0;
        long backoffMillis = 300L;
        try {
            while (true) {
                attempt++;
                try {
                    HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    int code = resp.statusCode();
                    long durMs = (System.nanoTime() - t0) / 1_000_000;
                    if (code / 100 == 2) {
                        log.info("send ok: {} ({} ms),bodyLen: {}", code, durMs, safeString(resp.body()));
                        return;
                    }
                    // 4xx — не ретраим
                    if (code / 100 == 4) {
                        String err = safeString(resp.body());
                        throw new IllegalStateException("HTTP " + code + " from " + endpoint + ": " + err);
                    }
                    // 5xx — возможно, ретраим
                    if (attempt >= maxAttempts) {
                        String err = safeString(resp.body());
                        throw new IllegalStateException("HTTP " + code + " after " + attempt + " attempts: " + err);
                    }
                } catch (IllegalStateException | IOException ex) {
                    if (attempt >= maxAttempts) throw ex;
                }

                Thread.sleep(backoffMillis);
                backoffMillis = Math.min(backoffMillis * 2, 5_000L);
            }
        } finally {
            // аккуратно восстановим прежний MDC
            if (prevMdc != null) org.slf4j.MDC.setContextMap(prevMdc);
            else org.slf4j.MDC.clear();
        }
    }

    private String buildIdempotencyKey(Envelope e) {
        return UUID.randomUUID().toString();
    }

    private byte[] serialize(Envelope e) throws Exception {
        log.info(e.toString());
        // Формируем корректное тело: branchId + payload по типу
        Object payload = switch (e.type()) {
            case GOODS -> new GoodsEnvelopeBody(e.branchId(), e.nom()); // e.nom(): NomenclatureIncoming
            case REST  -> new RestsEnvelopeBody(e.branchId(), e.ow());  // e.ow(): OffersWrapper
        };
        return mapper.writeValueAsBytes(payload);
    }

    private String toJson(Object o) throws JsonProcessingException {
        return o == null ? "null" : mapper.writeValueAsString(o);
    }
    private static String safeString(byte[] body) {
        if (body == null) return "";
        try { return new String(body, StandardCharsets.UTF_8); }
        catch (Exception ignore) { return ""; }
    }

    private record GoodsEnvelopeBody(String branchId, Object goods) {}
    private record RestsEnvelopeBody(String branchId, Object rests) {}
}
