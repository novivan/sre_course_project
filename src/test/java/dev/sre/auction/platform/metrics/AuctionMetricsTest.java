package dev.sre.auction.platform.metrics;

import static dev.sre.auction.platform.metrics.AuctionMetrics.BusinessOperation.PLACE_BID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import dev.sre.auction.platform.error.InvalidRequestException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuctionMetricsTest {

    @Test
    @DisplayName("records a successful business operation and returns its result")
    void recordBusinessOperationTestSuccess() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuctionMetrics sut = new AuctionMetrics(registry);

        // when
        String actual = sut.recordBusinessOperation(PLACE_BID, () -> "bid-id");

        // then
        assertThat(actual).isEqualTo("bid-id");
        assertThat(counter(registry, "success")).isEqualTo(1);
        assertThat(counter(registry, "rejected")).isZero();
        assertThat(counter(registry, "failure")).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void recordBusinessOperationTestException(
            String scenario,
            RuntimeException exception,
            String expectedResult
    ) {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuctionMetrics sut = new AuctionMetrics(registry);

        // then
        assertThatThrownBy(() -> sut.recordBusinessOperation(PLACE_BID, () -> {
            throw exception;
        })).isSameAs(exception);
        assertThat(counter(registry, expectedResult)).isEqualTo(1);
        assertThat(counter(registry, "success")).isZero();
    }

    static Stream<Arguments> recordBusinessOperationTestException() {
        return Stream.of(
                Arguments.of(
                        "records an expected business rejection",
                        new InvalidRequestException("INVALID_BID", "Bid is invalid"),
                        "rejected"
                ),
                Arguments.of(
                        "records an unexpected technical failure",
                        new IllegalStateException("Database is unavailable"),
                        "failure"
                )
        );
    }

    private static double counter(SimpleMeterRegistry registry, String result) {
        return registry.get("auction.business.operations")
                .tag("operation", "place_bid")
                .tag("result", result)
                .counter()
                .count();
    }
}
