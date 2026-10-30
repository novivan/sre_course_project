package dev.sre.auction.platform.probe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuctionProbeTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void recordsSuccessfulAndFailedChecks() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ok", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/failure", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProbeMetrics metrics = new ProbeMetrics(registry);

        probe("/ok", metrics).check();
        probe("/failure", metrics).check();

        assertThat(counter(registry, "success")).isEqualTo(1);
        assertThat(counter(registry, "failure")).isEqualTo(1);
        assertThat(registry.get("auction.probe.success").gauge().value()).isZero();
        assertThat(registry.get("auction.probe.duration").gauge().value())
                .isGreaterThanOrEqualTo(0);
    }

    private AuctionProbe probe(String path, ProbeMetrics metrics) {
        URI target = URI.create("http://localhost:" + server.getAddress().getPort() + path);
        return new AuctionProbe(target, Duration.ofSeconds(1), metrics);
    }

    private static double counter(SimpleMeterRegistry registry, String result) {
        return registry.get("auction.probe.checks")
                .tag("result", result)
                .counter()
                .count();
    }
}
