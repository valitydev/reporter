package dev.vality.reporter.data;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.domain_config_v2.VersionedObjectInfo;
import dev.vality.reporter.domain.enums.AdjustmentStatus;
import dev.vality.reporter.domain.enums.InvoicePaymentStatus;
import dev.vality.reporter.domain.enums.RefundStatus;
import dev.vality.reporter.domain.tables.pojos.*;

import java.time.LocalDateTime;
import java.util.List;

import static dev.vality.testcontainers.annotations.util.RandomBeans.random;

public class CommonTestData {

    private static final String DEFAULT_CURRENCY = "RUB";

    public static DomainObject getTestParty(String partyId, String shopId) {
        PartyConfig partyConfig = new PartyConfig();
        partyConfig.setName(random(String.class));
        partyConfig.setShops(List.of(getTestShopConfigRef(shopId)));
        partyConfig.setDescription(random(String.class));
        DomainObject domainObject = new DomainObject();
        domainObject.setPartyConfig(
                new PartyConfigObject()
                        .setData(partyConfig)
                        .setRef(new PartyConfigRef().setId(partyId)));
        return domainObject;
    }

    public static dev.vality.damsel.domain_config_v2.VersionedObject getVersionedObject(DomainObject domainObject) {
        var versionedObject = new dev.vality.damsel.domain_config_v2.VersionedObject();
        versionedObject.setInfo(new VersionedObjectInfo().setVersion(random(Integer.class)));
        versionedObject.setObject(domainObject);
        return versionedObject;
    }

    public static ShopConfigRef getTestShopConfigRef(String shopId) {
        ShopConfigRef shopConfigRef = new ShopConfigRef();
        shopConfigRef.setId(shopId);
        return shopConfigRef;
    }

    public static DomainObject getTestShop(String shopId) {
        ShopConfig shop = new ShopConfig();
        shop.setDescription(random(String.class));
        ShopLocation location = new ShopLocation();
        location.setUrl(random(String.class));
        shop.setLocation(location);
        shop.setName(random(String.class));
        DomainObject domainObject = new DomainObject();
        domainObject.setShopConfig(
                new ShopConfigObject()
                        .setData(shop)
                        .setRef(new ShopConfigRef().setId(shopId)));
        return domainObject;
    }

    public static DomainObject getTesCurrency(String code) {
        Currency currency = new Currency();
        currency.setName(random(String.class));
        currency.setExponent((short) 2);
        currency.setSymbolicCode(code);
        DomainObject domainObject = new DomainObject();
        domainObject.setCurrency(
                new CurrencyObject()
                        .setData(currency)
                        .setRef(new CurrencyRef().setSymbolicCode(code)));
        return domainObject;
    }

    public static Adjustment createTestAdjustment(String partyId,
                                                  String shopId,
                                                  LocalDateTime createdAt,
                                                  int i) {
        Adjustment adjustment = random(Adjustment.class);
        adjustment.setShopId(shopId + i % 2);
        adjustment.setPartyId(partyId + i % 2);
        adjustment.setCreatedAt(createdAt);
        adjustment.setStatusCreatedAt(createdAt);
        adjustment.setStatus(AdjustmentStatus.captured);
        adjustment.setAmount(1000L);
        adjustment.setCurrencyCode(DEFAULT_CURRENCY);
        return adjustment;
    }

    public static Refund createTestRefund(String partyId, String shopId, LocalDateTime createdAt, int i) {
        Refund refund = random(Refund.class);
        refund.setShopId(shopId + i % 2);
        refund.setPartyId(partyId + i % 2);
        refund.setCreatedAt(createdAt);
        refund.setStatusCreatedAt(createdAt);
        refund.setStatus(RefundStatus.succeeded);
        refund.setAmount(1000L);
        refund.setFee(500L);
        refund.setProviderFee(500L);
        refund.setExternalFee(500L);
        refund.setCurrencyCode(DEFAULT_CURRENCY);
        return refund;
    }

    public static Payment createTestPayment(String partyId, String shopId, LocalDateTime createdAt, int i) {
        Payment payment = random(Payment.class);
        payment.setShopId(shopId + i % 2);
        payment.setPartyId(partyId + i % 2);
        payment.setCreatedAt(createdAt);
        payment.setStatusCreatedAt(createdAt);
        payment.setStatus(InvoicePaymentStatus.captured);
        payment.setAmount(1000L);
        payment.setOriginAmount(1000L);
        payment.setFee(500L);
        payment.setProviderFee(500L);
        payment.setExternalFee(500L);
        payment.setCurrencyCode(DEFAULT_CURRENCY);
        return payment;
    }

    public static PaymentAggsByHour createTestPaymentAggsByHour(LocalDateTime createdAt,
                                                                String partyId,
                                                                String shopId) {
        return createTestPaymentAggsByHour(createdAt, partyId, shopId, 10000L, 2000L);
    }

    public static PaymentAggsByHour createTestPaymentAggsByHour(LocalDateTime createdAt,
                                                                String partyId,
                                                                String shopId,
                                                                Long amount,
                                                                Long fee) {
        PaymentAggsByHour paymentAggsByHour = new PaymentAggsByHour();
        paymentAggsByHour.setCreatedAt(createdAt);
        paymentAggsByHour.setPartyId(partyId);
        paymentAggsByHour.setShopId(shopId);
        paymentAggsByHour.setAmount(amount);
        paymentAggsByHour.setOriginAmount(amount);
        paymentAggsByHour.setCurrencyCode(DEFAULT_CURRENCY);
        paymentAggsByHour.setFee(fee);
        paymentAggsByHour.setProviderFee(fee);
        paymentAggsByHour.setExternalFee(fee);
        return paymentAggsByHour;
    }

    public static RefundAggsByHour createTestRefundAggsByHour(LocalDateTime createdAt,
                                                              String partyId,
                                                              String shopId) {
        return createTestRefundAggsByHour(createdAt, partyId, shopId, 10000L, 2000L);
    }

    public static RefundAggsByHour createTestRefundAggsByHour(LocalDateTime createdAt,
                                                              String partyId,
                                                              String shopId,
                                                              Long amount,
                                                              Long fee) {
        RefundAggsByHour refundAggsByHour = new RefundAggsByHour();
        refundAggsByHour.setCreatedAt(createdAt);
        refundAggsByHour.setPartyId(partyId);
        refundAggsByHour.setShopId(shopId);
        refundAggsByHour.setAmount(amount);
        refundAggsByHour.setCurrencyCode(DEFAULT_CURRENCY);
        refundAggsByHour.setFee(fee);
        refundAggsByHour.setProviderFee(fee);
        refundAggsByHour.setExternalFee(fee);
        return refundAggsByHour;
    }

    public static AdjustmentAggsByHour createTestAdjAggsByHour(LocalDateTime createdAt,
                                                               String partyId,
                                                               String shopId) {
        return createTestAdjAggsByHour(createdAt, partyId, shopId, 10000L);
    }

    public static AdjustmentAggsByHour createTestAdjAggsByHour(LocalDateTime createdAt,
                                                               String partyId,
                                                               String shopId,
                                                               Long amount) {
        AdjustmentAggsByHour adjustmentAggsByHour = new AdjustmentAggsByHour();
        adjustmentAggsByHour.setCreatedAt(createdAt);
        adjustmentAggsByHour.setPartyId(partyId);
        adjustmentAggsByHour.setShopId(shopId);
        adjustmentAggsByHour.setAmount(amount);
        adjustmentAggsByHour.setCurrencyCode(DEFAULT_CURRENCY);
        return adjustmentAggsByHour;
    }
}
