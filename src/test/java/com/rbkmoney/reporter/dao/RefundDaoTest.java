package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.PostgresqlSpringBootITest;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import org.jooq.Cursor;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.rbkmoney.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class RefundDaoTest {

    @Autowired
    private RefundDao refundDao;

    @Test
    public void saveAndGetRefundTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int refundsCount = 100;
        List<Refund> sourceRefunds = new ArrayList<>();
        for (int i = 0; i < refundsCount; i++) {
            Refund refund = random(Refund.class);
            refund.setShopId(shopId);
            refund.setPartyId(partyId);
            refund.setCreatedAt(LocalDateTime.now());
            refund.setStatusCreatedAt(LocalDateTime.now());
            refund.setStatus(RefundStatus.succeeded);
            sourceRefunds.add(refund);
            refundDao.saveRefund(refund);
        }

        Cursor<RefundRecord> refundsCursor = refundDao.getRefundsCursor(
                partyId,
                shopId,
                LocalDateTime.now().minus(10L, ChronoUnit.HOURS),
                LocalDateTime.now()
        );
        List<Refund> resultRefunds = new ArrayList<>();
        int iterationsCount = 0;
        while (refundsCursor.hasNext()) {
            Result<RefundRecord> refundRecords = refundsCursor.fetchNext(10);
            resultRefunds.addAll(refundRecords.into(Refund.class));
            iterationsCount++;
        }
        assertEquals(10, iterationsCount);
        assertEquals(refundsCount, resultRefunds.size());
    }
}
