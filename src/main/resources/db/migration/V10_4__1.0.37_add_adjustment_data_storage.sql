CREATE TYPE rpt.adjustment_status AS ENUM ('captured', 'cancelled');

CREATE TABLE rpt.adjustment
(
    id                             BIGSERIAL                   NOT NULL,
    party_id                       CHARACTER VARYING           NOT NULL,
    shop_id                        CHARACTER VARYING           NOT NULL,
    invoice_id                     CHARACTER VARYING           NOT NULL,
    payment_id                     CHARACTER VARYING           NOT NULL,
    adjustment_id                  CHARACTER VARYING           NOT NULL,
    created_at                     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status                         rpt.adjustment_status       NOT NULL,
    status_created_at              TIMESTAMP WITHOUT TIME ZONE,
    domain_revision                BIGINT,
    reason                         CHARACTER VARYING           NOT NULL,
    party_revision                 BIGINT,
    amount                         BIGINT,
    currency_code                  CHARACTER VARYING,
    CONSTRAINT adjustment_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX adjustment_id_idx on rpt.adjustment (invoice_id, payment_id, adjustment_id);
CREATE INDEX adjustment_created_at_idx ON rpt.adjustment (created_at);
CREATE INDEX adjustment_created_and_status_at_idx ON rpt.adjustment (status, created_at);
