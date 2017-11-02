package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.reporter.model.ShopAccountingModel;

import java.time.Instant;
import java.util.List;

/**
 * Created by tolkonepiu on 11/07/2017.
 */
public interface StatisticService {

    ShopAccountingModel getShopAccounting(String partyId, String shopId, Instant fromTime, Instant toTime);

    List<ShopAccountingModel> getShopAccountings(Instant fromTime, Instant toTime);

    List<StatPayment> getPayments(String partyId, String shopId, Instant fromTime, Instant toTime);

}
