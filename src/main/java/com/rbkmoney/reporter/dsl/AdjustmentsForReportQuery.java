package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class AdjustmentsForReportQuery {

    @JsonProperty("merchant_id")
    String merchantId;

    @JsonProperty("shop_id")
    String shopId;

    @JsonProperty("from_time")
    Instant fromTime;

    @JsonProperty("to_time")
    Instant toTime;

}
