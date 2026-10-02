package dev.sre.auction.platform.database;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DatabaseTime {

    private DatabaseTime() {
    }

    public static OffsetDateTime fromInstant(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
