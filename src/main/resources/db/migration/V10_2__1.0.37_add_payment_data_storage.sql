CREATE TYPE rpt.invoice_payment_status AS ENUM ('captured', 'cancelled', 'failed');
CREATE TYPE rpt.payment_tool AS ENUM ('bank_card', 'payment_terminal', 'digital_wallet', 'crypto_currency');
CREATE TYPE rpt.bank_card_token_provider AS ENUM ('applepay', 'googlepay', 'samsungpay');
CREATE TYPE rpt.payment_flow AS ENUM ('instant', 'hold');
CREATE TYPE rpt.on_hold_expiration AS ENUM ('cancel', 'capture');
CREATE TYPE rpt.payment_payer_type AS ENUM ('payment_resource', 'customer', 'recurrent');
CREATE TYPE rpt.failure_class AS ENUM ('operation_timeout', 'failure');

CREATE TABLE rpt.payment
(
    id                                BIGSERIAL                   NOT NULL,
    external_id                       CHARACTER VARYING           NOT NULL,
    party_id                          CHARACTER VARYING           NOT NULL,
    shop_id                           CHARACTER VARYING           NOT NULL,
    invoice_id                        CHARACTER VARYING           NOT NULL,
    payment_id                        CHARACTER VARYING           NOT NULL,
    amount                            BIGINT                      NOT NULL,
    origin_amount                     BIGINT,
    currency_code                     CHARACTER VARYING           NOT NULL,
    fee                               BIGINT,
    provider_fee                      BIGINT,
    external_fee                      BIGINT,
    created_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    payer_type                        rpt.payment_payer_type      NOT NULL,
    tool                              rpt.payment_tool            NOT NULL,
    status                            rpt.invoice_payment_status  NOT NULL,
    status_created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    phone_number                      CHARACTER VARYING,
    email                             CHARACTER VARYING,
    flow                              rpt.payment_flow            NOT NULL,
    context_type                      CHARACTER VARYING,
    context                           BYTEA,
    CONSTRAINT payment_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX payment_id_idx on rpt.payment (invoice_id, payment_id);
CREATE INDEX payment_created_at_idx ON rpt.payment (created_at);
CREATE INDEX payment_created_at_and_status_idx ON rpt.payment (status, created_at);

CREATE TABLE rpt.payment_additional_info
(
    ext_payment_id                    BIGINT                   NOT NULL,
    domain_revision                   BIGINT                   NOT NULL,
    party_revision                    BIGINT,
    provider_id                       INTEGER                  NOT NULL,
    terminal_id                       INTEGER                  NOT NULL,
    bank_card_token                   CHARACTER VARYING,
    bank_card_system                  CHARACTER VARYING,
    bank_card_bin                     CHARACTER VARYING,
    bank_card_masked_pan              CHARACTER VARYING,
    bank_card_token_provider          rpt.bank_card_token_provider,
    terminal_provider                 CHARACTER VARYING,
    digital_wallet_id                 CHARACTER VARYING,
    digital_wallet_provider           CHARACTER VARYING,
    session_id                        CHARACTER VARYING,
    fingerprint                       CHARACTER VARYING,
    ip                                CHARACTER VARYING,
    customer_id                       CHARACTER VARYING,
    recurrent_payer_parent_invoice_id CHARACTER VARYING,
    recurrent_payer_parent_payment_id CHARACTER VARYING,
    hold_on_expiration                rpt.on_hold_expiration,
    hold_until                        TIMESTAMP WITHOUT TIME ZONE,
    make_recurrent_flag               BOOLEAN,
    operation_failure_class           rpt.failure_class,
    external_failure                  CHARACTER VARYING,
    external_failure_reason           CHARACTER VARYING,
    payment_short_id                  CHARACTER VARYING,
    payer_crypto_currency_type        CHARACTER VARYING,
    rrn                               CHARACTER VARYING,
    approval_code                     CHARACTER VARYING,
    acs_url                           CHARACTER VARYING,
    pareq                             CHARACTER VARYING,
    md                                CHARACTER VARYING,
    term_url                          CHARACTER VARYING,
    pares                             CHARACTER VARYING,
    eci                               CHARACTER VARYING,
    cavv                              CHARACTER VARYING,
    xid                               CHARACTER VARYING,
    cavv_algorithm                    CHARACTER VARYING,
    three_ds_verification             CHARACTER VARYING,
    CONSTRAINT payment_additional_pkey PRIMARY KEY (ext_payment_id),
    FOREIGN KEY (ext_payment_id) REFERENCES rpt.payment (id)
);
