package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.VersionedObject;
import com.rbkmoney.geck.common.util.TypeUtil;

import java.time.Instant;
import java.util.Collections;

import static io.github.benas.randombeans.api.EnhancedRandom.random;

public class TestDataUtil {

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
        contract.setContractor(Contractor.legal_entity(LegalEntity.russian_legal_entity(russianLegalEntity)));
        contract.setLegalAgreement(new LegalAgreement(TypeUtil.temporalToString(Instant.now()), random(String.class)));
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
}
