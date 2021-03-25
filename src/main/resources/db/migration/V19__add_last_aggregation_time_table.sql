CREATE TYPE rpt.aggregation_type AS ENUM ('PAYMENT', 'REFUND', 'PAYOUT', 'ADJUSTMENT');
CREATE TYPE rpt.aggregation_interval AS ENUM ('HOUR', 'DAY', 'WEEK', 'MONTH');

CREATE TABLE rpt.last_aggregation_time
(
    aggregation_type             rpt.aggregation_type         NOT NULL,
    aggregation_interval         rpt.aggregation_interval     NOT NULL,
    last_data_aggregation_date   TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    last_act_time                TIMESTAMP WITHOUT TIME ZONE  NOT NULL  DEFAULT (now() at time zone 'utc'),

    CONSTRAINT last_aggregation_time_pkey PRIMARY KEY (aggregation_type, aggregation_interval)
);
