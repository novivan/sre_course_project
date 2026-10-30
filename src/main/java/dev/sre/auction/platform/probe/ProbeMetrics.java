package dev.sre.auction.platform.probe;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.role", havingValue = "worker")
public class ProbeMetrics {

    private final Counter successfulChecks;
    private final Counter failedChecks;
    private final AtomicInteger lastSuccess = new AtomicInteger();
    private final AtomicLong lastDurationNanos = new AtomicLong();

    public ProbeMetrics(MeterRegistry registry) {
        successfulChecks = counter(registry, "success");
        failedChecks = counter(registry, "failure");

        Gauge.builder("auction.probe.success", lastSuccess, AtomicInteger::get)
                .description("Result of the last synthetic check: 1 for success, 0 for failure")
                .register(registry);
        Gauge.builder(
                        "auction.probe.duration",
                        lastDurationNanos,
                        value -> value.get() / 1_000_000_000.0
                )
                .baseUnit("seconds")
                .description("Duration of the last synthetic check")
                .register(registry);
    }

    public void record(boolean successful, Duration duration) {
        lastSuccess.set(successful ? 1 : 0);
        lastDurationNanos.set(duration.toNanos());
        if (successful) {
            successfulChecks.increment();
        } else {
            failedChecks.increment();
        }
    }

    private static Counter counter(MeterRegistry registry, String result) {
        return Counter.builder("auction.probe.checks")
                .description("Synthetic API checks")
                .tag("result", result)
                .register(registry);
    }
}
