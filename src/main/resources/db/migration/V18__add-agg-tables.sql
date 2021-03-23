CREATE TABLE rpt.payment_aggs_by_hour
(
    id                                BIGSERIAL                   NOT NULL,
    created_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    party_id                          CHARACTER VARYING           NOT NULL,
    shop_id                           CHARACTER VARYING           NOT NULL,
    amount                            BIGINT                      NOT NULL,
    origin_amount                     BIGINT,
    currency_code                     CHARACTER VARYING           NOT NULL,
    fee                               BIGINT,
    provider_fee                      BIGINT,
    external_fee                      BIGINT,

    CONSTRAINT payment_aggs_by_hour_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX payment_aggs_by_hour_idx on rpt.payment_aggs_by_hour (party_id, shop_id, created_at, currency_code);
CREATE INDEX payment_aggs_by_hour_created_at_idx ON rpt.payment_aggs_by_hour (created_at);


CREATE TABLE rpt.refund_aggs_by_hour
(
    id                         BIGSERIAL                   NOT NULL,
    created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    party_id                   CHARACTER VARYING           NOT NULL,
    shop_id                    CHARACTER VARYING           NOT NULL,
    amount                     BIGINT                      NOT NULL,
    currency_code              CHARACTER VARYING           NOT NULL,
    fee                        BIGINT,
    provider_fee               BIGINT,
    external_fee               BIGINT,

    CONSTRAINT refund_aggs_by_hour_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX refund_aggs_by_hour_idx on rpt.refund_aggs_by_hour (party_id, shop_id, created_at, currency_code);
CREATE INDEX refund_aggs_by_hour_created_at_idx ON rpt.refund_aggs_by_hour (created_at);


CREATE TABLE rpt.adjustment_aggs_by_hour
(
    id                             BIGSERIAL                   NOT NULL,
    created_at                     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    party_id                       CHARACTER VARYING           NOT NULL,
    shop_id                        CHARACTER VARYING           NOT NULL,
    amount                         BIGINT,
    currency_code                  CHARACTER VARYING,
    CONSTRAINT adjustment_aggs_by_hour_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX adjustment_aggs_by_hour_idx on rpt.adjustment_aggs_by_hour (party_id, shop_id, created_at, currency_code);
CREATE INDEX adjustment_aggs_by_hour_created_at_idx ON rpt.adjustment_aggs_by_hour (created_at);


CREATE TABLE rpt.payout_aggs_by_hour
(
    id                         BIGSERIAL                   NOT NULL,
    created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    party_id                   CHARACTER VARYING           NOT NULL,
    shop_id                    CHARACTER VARYING           NOT NULL,
    amount                     BIGINT                      NOT NULL,
    currency_code              CHARACTER VARYING           NOT NULL,
    fee                        BIGINT                      NOT NULL,
    type                       rpt.payout_type             NOT NULL,

    CONSTRAINT payout_aggs_by_hour_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX payout_aggs_by_hour_idx on rpt.payout_aggs_by_hour (party_id, shop_id, created_at, type, currency_code);
CREATE INDEX payout_aggs_by_hour_created_at_idx ON rpt.payout_aggs_by_hour (created_at);
