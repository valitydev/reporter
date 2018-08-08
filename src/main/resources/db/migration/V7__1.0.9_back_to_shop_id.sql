ALTER TABLE rpt.report RENAME party_contract_id TO party_shop_id;
DELETE FROM rpt.report;
DELETE FROM rpt.file_meta;
UPDATE rpt.contract_meta SET last_closing_balance = null;