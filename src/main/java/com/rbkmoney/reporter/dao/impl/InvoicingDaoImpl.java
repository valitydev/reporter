package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.InvoicingDao;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoicingDaoImpl implements InvoicingDao {

    private final DSLContext dslContext;

    public InvoicingDaoImpl(HikariDataSource dataSource) {
        Configuration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(dataSource);
        this.dslContext = DSL.using(configuration);
    }

    @Override
    public void saveBatch(List<Query> invoicingQueries) {
        dslContext.batch(invoicingQueries).execute();
    }

}
