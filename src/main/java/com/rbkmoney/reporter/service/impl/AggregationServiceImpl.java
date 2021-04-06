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
@ConditionalOnProperty(value = "aggregation.enabled", havingValue = "true")
public class AggregationServiceImpl implements AggregationService {

    private static final long AGGREGATION_STEP = 6L;
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
        log.debug("Start '{}' aggregation", methodName);
        Optional<LocalDateTime> lastAggregationDateOptional = aggregatesDao.getLastAggregationDate(aggregationType);
        if (lastAggregationDateOptional.isEmpty()) {
            log.debug("Last '{}' aggregation time is empty", methodName);
            return;
        }
        LocalDateTime lastAggregationDate = lastAggregationDateOptional.get();

        log.debug("For '{}' aggregation last aggregation date is '{}'", methodName, lastAggregationDate);
        LocalDateTime highBordefOfAggregation;
        long untilNow = lastAggregationDate.until(LocalDateTime.now(), ChronoUnit.HOURS);
        if (untilNow == 0) {
            log.debug("Current time delta for '{}' aggregation less than one hour", methodName);
            return;
        } else if (untilNow < AGGREGATION_STEP) {
            highBordefOfAggregation = lastAggregationDate.plusHours(untilNow);
        } else {
            highBordefOfAggregation = lastAggregationDate.plusHours(AGGREGATION_STEP);
        }
        aggregatesDao.aggregateByHour(
                aggregationType,
                lastAggregationDate.minusHours(1L).truncatedTo(ChronoUnit.HOURS),
                highBordefOfAggregation.truncatedTo(ChronoUnit.HOURS)
        );
        aggregatesDao.saveLastAggregationDate(
                createLastAggregationTime(aggregationType, highBordefOfAggregation.minusHours(1L))
        );
        log.debug("'{}' aggregation was finished", methodName);
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
