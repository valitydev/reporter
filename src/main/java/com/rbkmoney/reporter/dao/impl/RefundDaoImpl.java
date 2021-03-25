package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.RefundDao;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.RefundAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.Refund.REFUND;
import static com.rbkmoney.reporter.domain.tables.RefundAdditionalInfo.REFUND_ADDITIONAL_INFO;

@Component
public class RefundDaoImpl extends AbstractDao implements RefundDao {

    @Autowired
    public RefundDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long saveRefund(Refund refund) {
        return getDslContext()
                .insertInto(REFUND)
                .set(getDslContext().newRecord(REFUND, refund))
                .onConflict(REFUND.INVOICE_ID, REFUND.PAYMENT_ID, REFUND.REFUND_ID)
                .doUpdate()
                .set(getDslContext().newRecord(REFUND, refund))
                .returning(REFUND.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public List<Refund> getRefundsByState(LocalDateTime dateFrom,
                                          LocalDateTime dateTo,
                                          List<RefundStatus> statuses) {
        Result<RefundRecord> records = getDslContext()
                .selectFrom(REFUND)
                .where(REFUND.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(REFUND.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(REFUND.STATUS.in(statuses)))
                .fetch();
        return records == null || records.isEmpty() ? new ArrayList<>() : records.into(Refund.class);
    }

    @Override
    public void saveAdditionalRefundInfo(RefundAdditionalInfo refundAdditionalInfo) {
        getDslContext()
                .insertInto(REFUND_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(REFUND_ADDITIONAL_INFO, refundAdditionalInfo))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(REFUND_ADDITIONAL_INFO, refundAdditionalInfo))
                .execute();
    }

    @Override
    public Cursor<RefundRecord> getRefundsCursor(String partyId,
                                                 String shopId,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime) {
        return getDslContext()
                .selectFrom(REFUND)
                .where(REFUND.STATUS_CREATED_AT.greaterThan(fromTime))
                .and(REFUND.STATUS_CREATED_AT.lessThan(toTime))
                .and(REFUND.PARTY_ID.eq(partyId))
                .and(REFUND.SHOP_ID.eq(shopId))
                .and(REFUND.STATUS.eq(RefundStatus.succeeded))
                .fetchLazy();
    }

}
