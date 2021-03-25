package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.reporter.dao.AggregatesDao;
import com.rbkmoney.reporter.domain.enums.AggregationInterval;
import com.rbkmoney.reporter.domain.enums.AggregationType;
import com.rbkmoney.reporter.domain.tables.pojos.LastAggregationTime;
import com.rbkmoney.reporter.service.AggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value="aggregation.enabled", havingValue = "true")
public class AggregationServiceImpl implements AggregationService {

    private final AggregatesDao aggregatesDao;

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregatePayments() {
        aggregateData(AggregationType.PAYMENT);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregateRefunds() {
        aggregateData(AggregationType.REFUND);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregateAdjustments() {
        aggregateData(AggregationType.ADJUSTMENT);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${aggregation.invoicing.timeout}")
    public void aggregatePayouts() {
        aggregateData(AggregationType.PAYOUT);
    }

    private void aggregateData(AggregationType aggregationType) {
        String methodName = aggregationType.getLiteral();
        log.info("Start '{}' aggregation", methodName);
        Optional<LocalDateTime> lastAggregationDateOptional = aggregatesDao.getLastAggregationDate(aggregationType);
        if (lastAggregationDateOptional.isEmpty()) {
            log.info("Last '{}' aggregation time is empty", methodName);
            return;
        }
        LocalDateTime lastAggregationDate = lastAggregationDateOptional.get();

        log.info("For '{}' aggregation last aggregation date is '{}'", methodName, lastAggregationDate);
        LocalDateTime now = LocalDateTime.now();
        long untilNow = lastAggregationDate.until(now, ChronoUnit.HOURS);
        if (untilNow == 0) {
            log.info("Current time delta for '{}' aggregation less than one hour", methodName);
            return;
        }
        aggregatesDao.aggregateByHour(
                aggregationType,
                lastAggregationDate.minusHours(3L),
                lastAggregationDate.plusHours(2L).truncatedTo(ChronoUnit.HOURS)
        );
        aggregatesDao.saveLastAggregationDate(
                createLastAggregationTime(aggregationType, lastAggregationDate.plusHours(1L))
        );
        log.info("'{}' aggregation was finished", methodName);
    }

    private LastAggregationTime createLastAggregationTime(AggregationType type,
                                                          LocalDateTime aggregationDate) {
        LastAggregationTime lastAggregationTime = new LastAggregationTime();
        lastAggregationTime.setAggregationType(type);
        lastAggregationTime.setAggregationInterval(AggregationInterval.HOUR);
        lastAggregationTime.setLastDataAggregationDate(aggregationDate);
        return lastAggregationTime;
    }

}
