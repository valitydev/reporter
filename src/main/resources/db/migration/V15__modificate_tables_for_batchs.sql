/** Remove old FK */
ALTER TABLE rpt.invoice_additional_info DROP CONSTRAINT invoice_additional_info_ext_invoice_id_fkey;
ALTER TABLE rpt.invoice_additional_info DROP COLUMN ext_invoice_id;
ALTER TABLE rpt.payment_additional_info DROP CONSTRAINT payment_additional_info_ext_payment_id_fkey;
ALTER TABLE rpt.payment_additional_info DROP COLUMN ext_payment_id;
ALTER TABLE rpt.refund_additional_info DROP CONSTRAINT refund_additional_info_ext_refund_id_fkey;
ALTER TABLE rpt.refund_additional_info DROP COLUMN ext_refund_id;

/** Add new FK */
ALTER TABLE rpt.invoice_additional_info ADD COLUMN id BIGSERIAL NOT NULL;
ALTER TABLE rpt.invoice_additional_info ADD COLUMN invoice_id CHARACTER VARYING NOT NULL;
CREATE UNIQUE INDEX invoice_additional_info_unq_idx ON rpt.invoice_additional_info (invoice_id);

ALTER TABLE rpt.payment_additional_info ADD COLUMN id BIGSERIAL NOT NULL;
ALTER TABLE rpt.payment_additional_info ADD COLUMN invoice_id CHARACTER VARYING NOT NULL;
ALTER TABLE rpt.payment_additional_info ADD COLUMN payment_id CHARACTER VARYING NOT NULL;
CREATE UNIQUE INDEX payment_additional_info_unq_idx ON rpt.payment_additional_info (invoice_id, payment_id);


ALTER TABLE rpt.refund_additional_info ADD COLUMN id BIGSERIAL NOT NULL;
ALTER TABLE rpt.refund_additional_info ADD COLUMN invoice_id CHARACTER VARYING NOT NULL;
ALTER TABLE rpt.refund_additional_info ADD COLUMN payment_id CHARACTER VARYING NOT NULL;
ALTER TABLE rpt.refund_additional_info ADD COLUMN refund_id CHARACTER VARYING NOT NULL;
CREATE UNIQUE INDEX refund_additional_info_unq_idx ON rpt.refund_additional_info (invoice_id, payment_id, refund_id);
