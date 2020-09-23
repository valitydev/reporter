package com.rbkmoney.reporter.dao;

import org.jooq.Query;

import java.util.List;

public interface InvoicingDao {

    void saveBatch(List<Query> invoicingQueries);

}
