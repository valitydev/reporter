CREATE TYPE rpt.chargeback_status AS ENUM ('pending', 'accepted', 'rejected', 'cancelled');

CREATE TYPE rpt.chargeback_category AS ENUM ('fraud', 'dispute', 'authorisation', 'processing_error');

CREATE TYPE rpt.chargeback_stage AS ENUM ('chargeback', 'pre_arbitration', 'arbitration');

CREATE TABLE rpt.chargeback
(
    id                 BIGSERIAL                   NOT NULL,
    domain_revision    BIGINT                      NOT NULL,
    party_revision     BIGINT,
    invoice_id         CHARACTER VARYING           NOT NULL,
    payment_id         CHARACTER VARYING           NOT NULL,
    chargeback_id      CHARACTER VARYING           NOT NULL,
    shop_id            CHARACTER VARYING           NOT NULL,
    party_id           CHARACTER VARYING           NOT NULL,
    external_id        CHARACTER VARYING,
    event_created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status             rpt.chargeback_status        NOT NULL,
    levy_amount        BIGINT,
    levy_currency_code CHARACTER VARYING,
    amount             BIGINT,
    currency_code      CHARACTER VARYING,
    reason_code        CHARACTER VARYING,
    reason_category    rpt.chargeback_category      NOT NULL,
    stage              rpt.chargeback_stage         NOT NULL,
    context            BYTEA,
    wtime              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),

    CONSTRAINT chargeback_pkey PRIMARY KEY (id)
);

ALTER TABLE rpt.chargeback
    ADD CONSTRAINT chargeback_uniq UNIQUE (invoice_id, payment_id, chargeback_id, stage);

CREATE INDEX chargeback_party_id on rpt.chargeback (party_id);
CREATE INDEX chargeback_status on rpt.chargeback (status);
CREATE INDEX chargeback_created_at on rpt.chargeback (created_at);
CREATE INDEX chargeback_event_created_at on rpt.chargeback (event_created_at, party_id, shop_id);
