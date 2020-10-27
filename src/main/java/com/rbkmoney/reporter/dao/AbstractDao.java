package com.rbkmoney.reporter.dao;

import lombok.Getter;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import javax.sql.DataSource;

@Getter
public abstract class AbstractDao {

    private final DSLContext dslContext;

    public AbstractDao(DataSource dataSource) {
        Configuration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(dataSource);
        this.dslContext = DSL.using(configuration);
    }

}
