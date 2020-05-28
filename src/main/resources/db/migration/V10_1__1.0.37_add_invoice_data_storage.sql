CREATE TYPE rpt.invoice_status AS ENUM ('unpaid', 'paid', 'cancelled', 'fulfilled');

CREATE TABLE rpt.invoice
(
    id                 BIGSERIAL                   NOT NULL,
    external_id        CHARACTER VARYING           NOT NULL,
    invoice_id         CHARACTER VARYING           NOT NULL,
    status             rpt.invoice_status          NOT NULL,
    status_created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    party_id           CHARACTER VARYING           NOT NULL,
    shop_id            CHARACTER VARYING           NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    product            CHARACTER VARYING           NOT NULL,
    description        CHARACTER VARYING,
    due                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount             BIGINT                      NOT NULL,
    currency_code      CHARACTER VARYING           NOT NULL,
    CONSTRAINT invoice_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX invoice_id_idx ON rpt.invoice (invoice_id);
CREATE INDEX invoice_created_at_idx ON rpt.invoice (created_at);
CREATE INDEX invoice_created_at_and_status_idx ON rpt.invoice (status, created_at);


CREATE TABLE rpt.invoice_additional_info
(
    ext_invoice_id BIGINT NOT NULL,
    status_details     CHARACTER VARYING,
    party_revision     BIGINT,
    cart_json          CHARACTER VARYING,
    context_type       CHARACTER VARYING,
    context            BYTEA,
    template_id        CHARACTER VARYING,
    CONSTRAINT invoice_additional_pkey PRIMARY KEY (ext_invoice_id),
    FOREIGN KEY (ext_invoice_id) REFERENCES rpt.invoice (id)
)
