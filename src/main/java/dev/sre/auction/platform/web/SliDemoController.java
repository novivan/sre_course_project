package dev.sre.auction.platform.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
@ConditionalOnProperty(name = "app.role", havingValue = "api", matchIfMissing = true)
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class SliDemoController {

    private static final long MAX_DELAY_MS = 5_000;

    @GetMapping
    public DemoResponse invoke(
            @RequestParam(defaultValue = "ok") String mode,
            @RequestParam(defaultValue = "1000") long delayMs
    ) {
        return switch (mode) {
            case "ok" -> new DemoResponse(mode, 0);
            case "slow" -> slow(delayMs);
            case "error" -> throw new IllegalStateException("Simulated error for SLI demo");
            default -> throw new IllegalArgumentException("Unknown demo mode: " + mode);
        };
    }

    private DemoResponse slow(long requestedDelayMs) {
        long delayMs = Math.clamp(requestedDelayMs, 0, MAX_DELAY_MS);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Demo request was interrupted", exception);
        }
        return new DemoResponse("slow", delayMs);
    }

    public record DemoResponse(String mode, long delayMs) {
    }
}
