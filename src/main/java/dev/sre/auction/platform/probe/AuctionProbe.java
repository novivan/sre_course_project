package dev.sre.auction.platform.probe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.role", havingValue = "worker")
public class AuctionProbe {

    private static final Logger log = LoggerFactory.getLogger(AuctionProbe.class);

    private final URI target;
    private final Duration timeout;
    private final ProbeMetrics metrics;
    private final HttpClient client;

    public AuctionProbe(
            @Value("${app.probe.url}") URI target,
            @Value("${app.probe.timeout:2s}") Duration timeout,
            ProbeMetrics metrics
    ) {
        this.target = target;
        this.timeout = timeout;
        this.metrics = metrics;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Scheduled(fixedDelayString = "${app.probe.interval:5s}")
    void check() {
        long startedAt = System.nanoTime();
        boolean successful = false;
        try {
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );
            successful = response.statusCode() >= 200 && response.statusCode() < 300;
            if (!successful) {
                log.warn("Synthetic check of {} returned status {}", target, response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Synthetic check of {} was interrupted", target);
        } catch (IOException | RuntimeException exception) {
            log.warn("Synthetic check of {} failed: {}", target, exception.getMessage());
        } finally {
            metrics.record(successful, Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }
}
