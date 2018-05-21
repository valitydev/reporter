CREATE TYPE rpt.REPORT_TYPE AS ENUM ('provision_of_service', 'payment_registry');

CREATE TABLE rpt.contract_meta (
  party_id                    CHARACTER VARYING                     NOT NULL,
  contract_id                 CHARACTER VARYING                     NOT NULL,
  report_type                 rpt.REPORT_TYPE                       NOT NULL,
  wtime                       TIMESTAMP WITHOUT TIME ZONE           NOT NULL,
  last_event_id               BIGINT                                NOT NULL,
  schedule_id                 INT,
  calendar_id                 INT,
  last_report_created_at      TIMESTAMP WITHOUT TIME ZONE,
  last_closing_balance        BIGINT,
  representative_position     CHARACTER VARYING,
  representative_full_name    CHARACTER VARYING,
  representative_document     CHARACTER VARYING,
  legal_agreement_id          CHARACTER VARYING,
  legal_agreement_signed_at   TIMESTAMP WITHOUT TIME ZONE,
  legal_agreement_valid_until TIMESTAMP WITHOUT TIME ZONE,
  CONSTRAINT contract_meta_pkey PRIMARY KEY (party_id, contract_id, report_type)
);

ALTER TABLE rpt.report
  ALTER COLUMN "type" TYPE rpt.REPORT_TYPE USING "type" :: TEXT :: rpt.REPORT_TYPE;
ALTER TABLE rpt.report
  DROP COLUMN need_sign;

DROP TABLE rpt.pos_report_meta;



