package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

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
        return Objects.equals(fromTime, that.fromTime) &&
                Objects.equals(toTime, that.toTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromTime, toTime);
    }

    @Override
    public String toString() {
        return "ShopAccountingQuery{" +
                "fromTime=" + fromTime +
                ", toTime=" + toTime +
                '}';
    }
}