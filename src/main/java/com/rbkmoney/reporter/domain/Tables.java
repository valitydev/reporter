/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.reporter.domain;


import com.rbkmoney.reporter.domain.tables.FileMeta;
import com.rbkmoney.reporter.domain.tables.Report;

import javax.annotation.Generated;


/**
 * Convenience access to all tables in rpt
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>rpt.file_meta</code>.
     */
    public static final FileMeta FILE_META = com.rbkmoney.reporter.domain.tables.FileMeta.FILE_META;

    /**
     * The table <code>rpt.report</code>.
     */
    public static final Report REPORT = com.rbkmoney.reporter.domain.tables.Report.REPORT;
}
