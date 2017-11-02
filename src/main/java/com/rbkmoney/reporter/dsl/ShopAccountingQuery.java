package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Created by tolkonepiu on 14/07/2017.
 */
public class ShopAccountingQuery {

    @JsonProperty("from_time")
    Instant fromTime;

    @JsonProperty("to_time")
    Instant toTime;

    public Instant getFromTime() {
        return fromTime;
    }

    public void setFromTime(Instant fromTime) {
        this.fromTime = fromTime;
    }

    public Instant getToTime() {
        return toTime;
    }

    public void setToTime(Instant toTime) {
        this.toTime = toTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShopAccountingQuery that = (ShopAccountingQuery) o;

        if (fromTime != null ? !fromTime.equals(that.fromTime) : that.fromTime != null) return false;
        return toTime != null ? toTime.equals(that.toTime) : that.toTime == null;
    }

    @Override
    public int hashCode() {
        int result = fromTime != null ? fromTime.hashCode() : 0;
        result = 31 * result + (toTime != null ? toTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ShopAccountingQuery{" +
                "fromTime=" + fromTime +
                ", toTime=" + toTime +
                '}';
    }
}
