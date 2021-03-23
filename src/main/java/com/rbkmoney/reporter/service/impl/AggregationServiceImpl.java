package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.service.AggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value="aggregation.enabled", havingValue = "true")
public class AggregationServiceImpl implements AggregationService {

    private final PaymentDao paymentDao;
    private final RefundDao refundDao;
    private final AdjustmentDao adjustmentDao;
    private final PayoutDao payoutDao;


    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregatePayments() {
        aggregateData(paymentDao, "Payment");
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregateRefunds() {
        aggregateData(refundDao, "Refund");
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregateAdjustments() {
        aggregateData(adjustmentDao, "Adjustment");
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregatePayouts() {
        aggregateData(payoutDao, "Payout");
    }

    private void aggregateData(AggregatesDao aggregatesDao, String methodName) {
        log.info("Start '{}' aggregation", methodName);
        LocalDateTime lastAggregationDate = aggregatesDao.getLastAggregationDate();
        if (lastAggregationDate == null) {
            return;
        }
        log.info("For '{}' aggregation last aggregation date is '{}'", methodName, lastAggregationDate);
        LocalDateTime now = LocalDateTime.now();
        long untilNow = lastAggregationDate.until(now, ChronoUnit.HOURS);
        if (untilNow == 0) {
            return;
        }
        aggregatesDao.aggregateForDate(lastAggregationDate.plusHours(1L), now.truncatedTo(ChronoUnit.HOURS));
        log.info("'{}' aggregation was finished", methodName);
    }

}
