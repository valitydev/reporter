CREATE TYPE rpt.refund_status AS ENUM ('succeeded', 'failed');

CREATE TABLE rpt.refund
(
    id                         BIGSERIAL                   NOT NULL,
    external_id                CHARACTER VARYING           NOT NULL,
    party_id                   CHARACTER VARYING           NOT NULL,
    shop_id                    CHARACTER VARYING           NOT NULL,
    invoice_id                 CHARACTER VARYING           NOT NULL,
    payment_id                 CHARACTER VARYING           NOT NULL,
    refund_id                  CHARACTER VARYING           NOT NULL,
    created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount                     BIGINT                      NOT NULL,
    currency_code              CHARACTER VARYING           NOT NULL,
    reason                     CHARACTER VARYING,
    status                     rpt.refund_status           NOT NULL,
    status_created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fee                        BIGINT,
    provider_fee               BIGINT,
    external_fee               BIGINT,
    CONSTRAINT refund_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX refund_id_idx on rpt.refund (invoice_id, payment_id, refund_id);
CREATE INDEX refund_created_at_idx ON rpt.refund (created_at);
CREATE INDEX refund_created_at_and_status_idx ON rpt.refund (status, created_at);


CREATE TABLE rpt.refund_additional_info
(
    ext_refund_id                         BIGINT                   NOT NULL,
    operation_failure_class               rpt.failure_class,
    external_failure                      CHARACTER VARYING,
    external_failure_reason               CHARACTER VARYING,
    domain_revision                       BIGINT,
    party_revision                        BIGINT,
    CONSTRAINT refund_additional_pkey PRIMARY KEY (ext_refund_id),
    FOREIGN KEY (ext_refund_id) REFERENCES rpt.refund (id)
)
