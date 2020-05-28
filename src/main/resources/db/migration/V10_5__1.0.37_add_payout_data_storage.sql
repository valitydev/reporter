CREATE TYPE rpt.payout_status AS ENUM ('unpaid', 'paid', 'cancelled', 'confirmed');
CREATE TYPE rpt.payout_type AS ENUM ('bank_card', 'bank_account', 'wallet');
CREATE TYPE rpt.payout_account_type AS ENUM ('russian_payout_account', 'international_payout_account');

CREATE TABLE rpt.payout
(
    id                                                    BIGSERIAL                   NOT NULL,
    party_id                                              CHARACTER VARYING           NOT NULL,
    shop_id                                               CHARACTER VARYING           NOT NULL,
    payout_id                                             CHARACTER VARYING           NOT NULL,
    contract_id                                           CHARACTER VARYING           NOT NULL,
    created_at                                            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount                                                BIGINT                      NOT NULL,
    fee                                                   BIGINT                      NOT NULL,
    currency_code                                         CHARACTER VARYING           NOT NULL,
    type                                                  rpt.payout_type             NOT NULL,
    wallet_id                                             CHARACTER VARYING,
    summary                                               CHARACTER VARYING,
    CONSTRAINT payout_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX payout_id_idx on rpt.payout (payout_id);
CREATE INDEX payout_created_at_idx ON rpt.payout (created_at);

CREATE TABLE rpt.payout_account (
    ext_payout_id               BIGINT                    NOT NULL,
    type                        rpt.payout_account_type,
    bank_id                     CHARACTER VARYING,
    bank_corr_id                CHARACTER VARYING,
    bank_local_code             CHARACTER VARYING,
    bank_name                   CHARACTER VARYING,
    purpose                     CHARACTER VARYING,
    inn                         CHARACTER VARYING,
    legal_agreement_id          CHARACTER VARYING,
    legal_agreement_signed_at   TIMESTAMP WITHOUT TIME ZONE,
    trading_name                CHARACTER VARYING,
    legal_name                  CHARACTER VARYING,
    actual_address              CHARACTER VARYING,
    registered_address          CHARACTER VARYING,
    registered_number           CHARACTER VARYING,
    bank_iban                   CHARACTER VARYING,
    bank_number                 CHARACTER VARYING,
    bank_address                CHARACTER VARYING,
    bank_bic                    CHARACTER VARYING,
    bank_aba_rtn                CHARACTER VARYING,
    bank_country_code           CHARACTER VARYING,
    CONSTRAINT payout_account_pkey PRIMARY KEY (ext_payout_id),
    FOREIGN KEY (ext_payout_id) REFERENCES rpt.payout (id)
);

CREATE TABLE rpt.payout_international_account (
    ext_payout_id               BIGINT                    NOT NULL,
    bank_account                CHARACTER VARYING,
    bank_number                 CHARACTER VARYING,
    bank_iban                   CHARACTER VARYING,
    bank_name                   CHARACTER VARYING,
    bank_address                CHARACTER VARYING,
    bank_bic                    CHARACTER VARYING,
    bank_aba_rtn                CHARACTER VARYING,
    bank_country_code           CHARACTER VARYING,
    CONSTRAINT payout_international_account_pkey PRIMARY KEY (ext_payout_id),
    FOREIGN KEY (ext_payout_id) REFERENCES rpt.payout (id)
);

CREATE TABLE rpt.payout_state
(
    id               BIGSERIAL                   NOT NULL,
    event_id         BIGINT                      NOT NULL,
    event_created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    payout_id        CHARACTER VARYING           NOT NULL,
    ext_payout_id    CHARACTER VARYING           NOT NULL,
    status           rpt.payout_status           NOT NULL,
    cancel_details   CHARACTER VARYING,
    CONSTRAINT payout_status_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX payout_status_idx ON rpt.payout_state (ext_payout_id, event_created_at, status);
CREATE INDEX payout_status_by_date_idx ON rpt.payout_state (event_created_at, status);
