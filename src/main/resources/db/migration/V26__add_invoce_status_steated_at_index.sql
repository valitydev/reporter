CREATE INDEX IF NOT EXISTS invoice_status_created_at_brin
    ON rpt.invoice USING brin(status_created_at) WITH (pages_per_range='1');
