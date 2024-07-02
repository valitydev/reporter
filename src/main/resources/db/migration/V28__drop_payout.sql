DROP TABLE IF EXISTS rpt.payout_account;
DROP TABLE IF EXISTS rpt.payout_international_account;
DROP TABLE IF EXISTS rpt.payout_state;
DROP TABLE IF EXISTS rpt.payout;
DROP TABLE IF EXISTS rpt.payout_aggs_by_hour;
DROP TYPE IF EXISTS rpt.payout_status;
DROP TYPE IF EXISTS rpt.payout_type;
DROP TYPE IF EXISTS rpt.payout_account_type;
DROP TYPE IF EXISTS rpt.payout_account_type;

-- rename the existing type
ALTER TYPE rpt.REPORT_TYPE RENAME TO REPORT_TYPE_OLD;
--  create the new type
CREATE TYPE rpt.REPORT_TYPE AS ENUM('provision_of_service', 'payment_registry', 'local_payment_registry', 'local_provision_of_service');
--  update the columns to use the new type
ALTER TABLE rpt.report ALTER COLUMN "type" TYPE rpt.REPORT_TYPE USING "type" :: TEXT :: rpt.REPORT_TYPE;
ALTER TABLE rpt.report_comparing_data ALTER COLUMN report_type TYPE rpt.REPORT_TYPE USING report_type :: TEXT :: rpt.REPORT_TYPE;
-- remove the old type
DROP TYPE rpt.REPORT_TYPE_OLD;


ALTER TYPE rpt.AGGREGATION_TYPE RENAME TO AGGREGATION_TYPE_OLD;
CREATE TYPE rpt.AGGREGATION_TYPE AS ENUM('PAYMENT', 'REFUND', 'ADJUSTMENT');
ALTER TABLE rpt.last_aggregation_time ALTER COLUMN aggregation_type TYPE rpt.AGGREGATION_TYPE USING aggregation_type :: TEXT :: rpt.AGGREGATION_TYPE;
DROP TYPE rpt.AGGREGATION_TYPE_OLD;