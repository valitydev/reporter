package com.rbkmoney.reporter.dsl;

/**
 * Created by tolkonepiu on 10/07/2017.
 */
public class StatisticDsl {

    private Query query;

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatisticDsl that = (StatisticDsl) o;

        return query != null ? query.equals(that.query) : that.query == null;
    }

    @Override
    public int hashCode() {
        return query != null ? query.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "StatisticDsl{" +
                "query=" + query +
                '}';
    }
}
