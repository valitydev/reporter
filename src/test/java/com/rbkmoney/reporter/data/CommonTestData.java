package com.rbkmoney.reporter.data;

import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.VersionedObject;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.enums.*;
import com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;

import static io.github.benas.randombeans.api.EnhancedRandom.random;

public class CommonTestData {

    private static final String DEFAULT_CURRENCY = "RUB";

    public static Party getTestParty(String partyId, String shopId, String contractId) {
        Party party = new Party();
        party.setId(partyId);

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setPaymentInstitution(new PaymentInstitutionRef(1));
        RussianLegalEntity russianLegalEntity = new RussianLegalEntity();
        russianLegalEntity.setRegisteredName(random(String.class));
        russianLegalEntity.setRepresentativePosition(random(String.class));
        russianLegalEntity.setRepresentativeFullName(random(String.class));
        contract.setContractor(
                Contractor.legal_entity(LegalEntity.russian_legal_entity(russianLegalEntity)));
        contract.setLegalAgreement(
                new LegalAgreement(TypeUtil.temporalToString(Instant.now()), random(String.class)));
        party.setShops(Collections.singletonMap(shopId, getTestShop(shopId, contractId)));
        party.setContracts(Collections.singletonMap(contractId, contract));
        return party;
    }

    public static Shop getTestShop(String shopId, String contractId) {
        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setContractId(contractId);
        shop.setLocation(ShopLocation.url("http://2ch.hk/"));
        return shop;
    }

    public static VersionedObject buildPaymentCalendarObject(CalendarRef calendarRef) {
        Calendar calendar = new Calendar("calendar", "Europe/Moscow", Collections.emptyMap());

        return new VersionedObject(
                1,
                DomainObject.calendar(new CalendarObject(
                        calendarRef,
                        calendar
                ))
        );
    }

    public static VersionedObject buildPaymentInstitutionObject(PaymentInstitutionRef paymentInstitutionRef) {
        PaymentInstitution paymentInstitution = new PaymentInstitution();
        paymentInstitution.setCalendar(new CalendarRef(1));

        return new VersionedObject(
                1,
                DomainObject.payment_institution(new PaymentInstitutionObject(
                        paymentInstitutionRef,
                        paymentInstitution
                ))
        );
    }

    public static VersionedObject buildPayoutScheduleObject(BusinessScheduleRef payoutScheduleRef) {
        ScheduleEvery nth5 = new ScheduleEvery();
        nth5.setNth((byte) 5);

        BusinessSchedule payoutSchedule = new BusinessSchedule();
        payoutSchedule.setName("schedule");
        payoutSchedule.setSchedule(new Schedule(
                ScheduleYear.every(new ScheduleEvery()),
                ScheduleMonth.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleDayOfWeek.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery(nth5))
        ));
        payoutSchedule.setPolicy(new PayoutCompilationPolicy(new TimeSpan()));

        return new VersionedObject(
                1,
                DomainObject.business_schedule(new BusinessScheduleObject(
                        payoutScheduleRef,
                        payoutSchedule
                ))
        );
    }

    public static PayoutState createTestPayoutState(Long extPayoutId,
                                                    LocalDateTime createdAt,
                                                    PayoutStatus status,
                                                    int i) {
        PayoutState payoutState = random(PayoutState.class);
        payoutState.setId(null);
        payoutState.setExtPayoutId(extPayoutId);
        payoutState.setEventCreatedAt(createdAt);
        payoutState.setStatus(status);
        payoutState.setPayoutId("payout." + i);
        return payoutState;
    }

    public static Payout createTestPayout(String partyId,
                                          String shopId,
                                          LocalDateTime createdAt,
                                          int i) {
        Payout payout = random(Payout.class);
        payout.setId(null);
        payout.setPayoutId("payout." + i);
        payout.setShopId(shopId + i % 2);
        payout.setPartyId(partyId + i % 2);
        payout.setCreatedAt(createdAt);
        payout.setAmount(1000L);
        payout.setCurrencyCode("RUB");
        payout.setFee(500L);
        payout.setType(PayoutType.bank_card);
        return payout;
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

    public static PayoutAggsByHour createTestPayoutAggsByHour(LocalDateTime createdAt,
                                                              String partyId,
                                                              String shopId) {
        return createTestPayoutAggsByHour(createdAt, partyId, shopId, 10000L, 2000L);
    }

    public static PayoutAggsByHour createTestPayoutAggsByHour(LocalDateTime createdAt,
                                                              String partyId,
                                                              String shopId,
                                                              Long amount,
                                                              Long fee) {
        PayoutAggsByHour payoutAggsByHour = new PayoutAggsByHour();
        payoutAggsByHour.setCreatedAt(createdAt);
        payoutAggsByHour.setPartyId(partyId);
        payoutAggsByHour.setShopId(shopId);
        payoutAggsByHour.setAmount(amount);
        payoutAggsByHour.setCurrencyCode(DEFAULT_CURRENCY);
        payoutAggsByHour.setFee(fee);
        payoutAggsByHour.setType(PayoutType.bank_card);
        return payoutAggsByHour;
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
