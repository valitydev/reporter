package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.mapper.RecordRowMapper;
import com.rbkmoney.reporter.dao.RefundDao;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.RefundAdditionalInfo;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.Refund.REFUND;
import static com.rbkmoney.reporter.domain.tables.RefundAdditionalInfo.REFUND_ADDITIONAL_INFO;

@Component
public class RefundDaoImpl extends AbstractGenericDao implements RefundDao {

    private final RowMapper<Refund> refundRowMapper;

    @Autowired
    public RefundDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        refundRowMapper = new RecordRowMapper<>(REFUND, Refund.class);
    }

    @Override
    public Long saveRefund(Refund refund) {
        return fetchOne(getSaveRefundQuery(refund), Long.class);
    }

    @Override
    public Query getSaveRefundQuery(Refund refund) {
        return getDslContext()
                .insertInto(REFUND)
                .set(getDslContext().newRecord(REFUND, refund))
                .onConflict(REFUND.INVOICE_ID, REFUND.PAYMENT_ID, REFUND.REFUND_ID)
                .doNothing();
    }

    @Override
    public List<Refund> getRefundsByState(LocalDateTime dateFrom,
                                          LocalDateTime dateTo,
                                          List<RefundStatus> statuses) {
        Query query = getDslContext()
                .selectFrom(REFUND)
                .where(REFUND.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(REFUND.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(REFUND.STATUS.in(statuses)));
        return fetch(query, refundRowMapper);
    }

    @Override
    public void saveAdditionalRefundInfo(RefundAdditionalInfo refundAdditionalInfo) {
        executeOne(
                getSaveAdditionalRefundInfoQuery(refundAdditionalInfo)
        );
    }

    @Override
    public Query getSaveAdditionalRefundInfoQuery(RefundAdditionalInfo refundAdditionalInfo) {
        return getDslContext()
                .insertInto(REFUND_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(REFUND_ADDITIONAL_INFO, refundAdditionalInfo))
                .onConflict(
                        REFUND_ADDITIONAL_INFO.INVOICE_ID,
                        REFUND_ADDITIONAL_INFO.PAYMENT_ID,
                        REFUND_ADDITIONAL_INFO.REFUND_ID)
                .doNothing();
    }
}
