ALTER TABLE rpt.invoice ALTER COLUMN shop_id DROP NOT NULL;
ALTER TABLE rpt.payment ALTER COLUMN shop_id DROP NOT NULL;
ALTER TABLE rpt.refund  ALTER COLUMN shop_id DROP NOT NULL;
ALTER TABLE rpt.adjustment  ALTER COLUMN party_id DROP NOT NULL;
ALTER TABLE rpt.adjustment  ALTER COLUMN shop_id DROP NOT NULL;

ALTER TABLE rpt.payment_additional_info ALTER COLUMN domain_revision DROP NOT NULL;
ALTER TABLE rpt.payment_additional_info ALTER COLUMN provider_id DROP NOT NULL;
ALTER TABLE rpt.payment_additional_info ALTER COLUMN terminal_id DROP NOT NULL;

