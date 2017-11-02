package com.rbkmoney.reporter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.merch_stat.MerchantStatisticsSrv;
import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.reporter.dsl.DslUtil;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.StatisticService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticServiceImpl implements StatisticService {

    private final MerchantStatisticsSrv.Iface merchantStatisticsClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public StatisticServiceImpl(MerchantStatisticsSrv.Iface merchantStatisticsClient, ObjectMapper objectMapper) {
        this.merchantStatisticsClient = merchantStatisticsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ShopAccountingModel getShopAccounting(String partyId, String shopId, Instant fromTime, Instant toTime) {
        return getShopAccountings(fromTime, toTime).stream().filter(
                shopAccountingModel -> shopAccountingModel.getMerchantId().equals(partyId)
                        && shopAccountingModel.getShopId().equals(shopId)
        ).findFirst().orElse(new ShopAccountingModel());
    }

    @Override
    public List<ShopAccountingModel> getShopAccountings(Instant fromTime, Instant toTime) {
        try {
            return merchantStatisticsClient.getStatistics(DslUtil.createShopAccountingStatRequest(fromTime, toTime, objectMapper))
                    .getData()
                    .getRecords()
                    .stream()
                    .map(record -> objectMapper.convertValue(record, ShopAccountingModel.class))
                    .collect(Collectors.toList());
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<StatPayment> getPayments(String partyId, String shopId, Instant fromTime, Instant toTime) {
        try {
            return merchantStatisticsClient.getPayments(DslUtil.createPaymentsRequest(partyId, shopId, fromTime, toTime, objectMapper))
                    .getData().getPayments();
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }
}
