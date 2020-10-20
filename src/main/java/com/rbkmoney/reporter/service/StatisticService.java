package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.merch_stat.StatInvoice;
import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.model.StatAdjustment;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by tolkonepiu on 11/07/2017.
 */
public interface StatisticService {

    ShopAccountingModel getShopAccounting(String partyId, String shopId, String currencyCode, Instant toTime);

    ShopAccountingModel getShopAccounting(String partyId, String shopId, String currencyCode, Instant fromTime, Instant toTime);

    Map<String, String> getPurposes(String partyId, String shopId, Instant fromTime, Instant toTime);

    StatInvoice getInvoice(String invoiceId);

    Iterator<StatPayment> getCapturedPaymentsIterator(String partyId, String shopId, Instant fromTime, Instant toTime);

    StatPayment getCapturedPayment(String partyId, String shopId, String invoiceId, String paymentId);

    Iterator<StatRefund> getRefundsIterator(String partyId, String shopId, Instant fromTime, Instant toTime);

    Iterator<StatAdjustment> getAdjustmentsIterator(String partyId, String shopId, Instant fromTime, Instant toTime);

}
