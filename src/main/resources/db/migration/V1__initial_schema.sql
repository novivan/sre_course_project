CREATE TABLE users (
    id          UUID PRIMARY KEY,
    username    VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE lots (
    id           UUID PRIMARY KEY,
    seller_id    UUID NOT NULL REFERENCES users(id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    image_url    TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE auctions (
    id                   UUID PRIMARY KEY,
    lot_id               UUID NOT NULL REFERENCES lots(id),
    status               VARCHAR(32) NOT NULL,
    currency             CHAR(3) NOT NULL DEFAULT 'RUB',
    start_price_minor    BIGINT NOT NULL,
    current_price_minor  BIGINT NOT NULL,
    min_increment_minor  BIGINT NOT NULL,
    starts_at            TIMESTAMPTZ NOT NULL,
    ends_at              TIMESTAMPTZ NOT NULL,
    current_leader_id    UUID REFERENCES users(id),
    winner_id            UUID REFERENCES users(id),
    version              BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT auctions_status_check CHECK (
        status IN ('SCHEDULED', 'ACTIVE', 'FINISHED', 'CANCELLED')
    ),
    CONSTRAINT auctions_prices_check CHECK (
        start_price_minor >= 0
        AND current_price_minor >= start_price_minor
        AND min_increment_minor > 0
    ),
    CONSTRAINT auctions_window_check CHECK (ends_at > starts_at)
);

CREATE TABLE bids (
    id               UUID PRIMARY KEY,
    auction_id       UUID NOT NULL REFERENCES auctions(id),
    bidder_id        UUID NOT NULL REFERENCES users(id),
    amount_minor     BIGINT NOT NULL CHECK (amount_minor > 0),
    idempotency_key  UUID NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT bids_idempotency_unique UNIQUE (
        auction_id,
        bidder_id,
        idempotency_key
    )
);

CREATE INDEX lots_by_seller_idx
    ON lots (seller_id, created_at DESC);

CREATE INDEX auctions_catalog_idx
    ON auctions (status, ends_at);

CREATE INDEX auctions_closing_idx
    ON auctions (ends_at)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX auctions_one_open_per_lot_idx
    ON auctions (lot_id)
    WHERE status IN ('SCHEDULED', 'ACTIVE');

CREATE INDEX bids_history_idx
    ON bids (auction_id, created_at DESC, id DESC);
