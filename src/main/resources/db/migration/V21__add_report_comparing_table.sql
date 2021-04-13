CREATE TYPE rpt.comparing_status AS ENUM ('SUCCESS', 'FAILED');

CREATE TABLE rpt.report_comparing_data
(
    report_id       BIGINT                     NOT NULL,
    report_type     rpt.REPORT_TYPE            NOT NULL,
    status          rpt.comparing_status       NOT NULL,
    failure_reason  CHARACTER VARYING,

    CONSTRAINT report_comparing_data_pkey PRIMARY KEY (report_id, report_type)
);
