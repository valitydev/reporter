ALTER TABLE rpt.payout_state ALTER COLUMN ext_payout_id TYPE BIGINT USING ext_payout_id::bigint;

ALTER TABLE rpt.invoice ALTER COLUMN party_id DROP NOT NULL;
ALTER TABLE rpt.payment ALTER COLUMN party_id DROP NOT NULL;
ALTER TABLE rpt.refund  ALTER COLUMN party_id DROP NOT NULL;
