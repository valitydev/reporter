CREATE TABLE rpt.pos_report_meta (
  party_id               CHARACTER VARYING NOT NULL,
  contract_id            CHARACTER VARYING NOT NULL,
  last_opening_balance   BIGINT,
  last_closing_balance   BIGINT,
  last_report_created_at TIMESTAMP WITHOUT TIME ZONE,
  CONSTRAINT pos_report_meta_pkey PRIMARY KEY (party_id, contract_id)
);