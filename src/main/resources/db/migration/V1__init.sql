CREATE SCHEMA IF NOT EXISTS rpt;

CREATE TYPE rpt.REPORT_STATUS AS ENUM ('pending', 'created');

CREATE TABLE rpt.report (
  id            BIGSERIAL                   NOT NULL,
  from_time     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  to_time       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  party_id      CHARACTER VARYING           NOT NULL,
  party_shop_id CHARACTER VARYING           NOT NULL,
  status        rpt.REPORT_STATUS           NOT NULL DEFAULT 'pending' :: rpt.REPORT_STATUS,
  timezone      CHARACTER VARYING           NOT NULL,
  type          CHARACTER VARYING           NOT NULL,
  CONSTRAINT report_pkey PRIMARY KEY (id)
);

CREATE TABLE rpt.file_meta (
  file_id   CHARACTER VARYING NOT NULL,
  bucket_id CHARACTER VARYING NOT NULL,
  report_id BIGINT            NOT NULL,
  filename  CHARACTER VARYING NOT NULL,
  md5       CHARACTER VARYING NOT NULL,
  sha256    CHARACTER VARYING NOT NULL,
  CONSTRAINT file_meta_pkey PRIMARY KEY (bucket_id, file_id)
);


