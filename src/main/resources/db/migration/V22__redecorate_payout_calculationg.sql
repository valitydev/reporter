truncate table rpt.payout_aggs_by_hour;
truncate table rpt.report_comparing_data;

ALTER TABLE rpt.payout_aggs_by_hour DROP COLUMN fee;
