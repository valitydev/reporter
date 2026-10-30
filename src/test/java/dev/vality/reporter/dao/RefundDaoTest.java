package dev.vality.reporter.dao;

import dev.vality.reporter.config.PostgresqlSpringBootITest;
import dev.vality.reporter.domain.enums.RefundStatus;
import dev.vality.reporter.domain.tables.pojos.Refund;
import dev.vality.reporter.domain.tables.records.RefundRecord;
import org.jooq.Cursor;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static dev.vality.reporter.util.RandomBeans.random;
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
        List<Refund> sourceRefunds = IntStream.range(0, refundsCount)
                .mapToObj(i -> random(Refund.class))
                .toList();
        sourceRefunds.forEach(refund -> {
            refund.setShopId(shopId);
            refund.setPartyId(partyId);
            refund.setCreatedAt(LocalDateTime.now());
            refund.setStatusCreatedAt(LocalDateTime.now());
            refund.setStatus(RefundStatus.succeeded);
            refundDao.saveRefund(refund);
        });

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
