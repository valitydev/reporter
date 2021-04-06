CREATE INDEX IF NOT EXIST payment_status_created_at_idx ON rpt.payment (status, status_created_at);
CREATE INDEX IF NOT EXIST refund_status_created_at_idx ON rpt.refund (status, status_created_at);
CREATE INDEX IF NOT EXIST adjustment_status_created_at_idx ON rpt.adjustment (status, status_created_at);