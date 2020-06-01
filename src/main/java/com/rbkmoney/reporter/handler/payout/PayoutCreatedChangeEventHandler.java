package com.rbkmoney.reporter.handler.payout;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.dao.PayoutDao;
import com.rbkmoney.reporter.domain.enums.PayoutAccountType;
import com.rbkmoney.reporter.domain.enums.PayoutStatus;
import com.rbkmoney.reporter.domain.enums.PayoutType;
import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutInternationalAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
import com.rbkmoney.reporter.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutCreatedChangeEventHandler extends AbstractPayoutHandler {

    private final PayoutDao payoutDao;

    @Override
    public void handle(PayoutChange payload, Event event) {
        var damselPayout = payload.getPayoutCreated().getPayout();
        var damselPayoutType = damselPayout.getType();
        String payoutId = event.getSource().getPayoutId();

        log.info("Start payout created handling, payoutId={}", payoutId);

        Payout payout = getPayout(damselPayout, damselPayoutType, payoutId);
        PayoutState payoutState = getPayoutState(event, damselPayout, payoutId);
        Long extPayoutId = payoutDao.savePayout(payout);
        payoutDao.savePayoutState(payoutState);

        if (damselPayoutType.isSetBankAccount()) {
            PayoutAccount payoutAccount = damselPayoutType.getBankAccount();

            if (payoutAccount.isSetRussianPayoutAccount()) {
                saveRussianPayoutAccount(payoutAccount, extPayoutId);
            } else if (payoutAccount.isSetInternationalPayoutAccount()) {
                saveInternationPayoutAccount(payoutAccount, extPayoutId);
            }
        }

        log.info("Payout has been created, payoutId={}", payoutId);
    }

    @Override
    public boolean accept(PayoutChange payload) {
        return payload.isSetPayoutCreated();
    }

    private Payout getPayout(com.rbkmoney.damsel.payout_processing.Payout damselPayout,
                             com.rbkmoney.damsel.payout_processing.PayoutType damselPayoutType,
                             String payoutId) {
        Payout payout = new Payout();
        payout.setPartyId(damselPayout.getPartyId());
        payout.setShopId(damselPayout.getShopId());
        payout.setPayoutId(payoutId);
        payout.setContractId(damselPayout.getContractId());
        payout.setCreatedAt(TypeUtil.stringToLocalDateTime(damselPayout.getCreatedAt()));
        payout.setAmount(damselPayout.getAmount());
        payout.setFee(damselPayout.getFee());
        payout.setCurrencyCode(damselPayout.getCurrency().getSymbolicCode());
        payout.setType(TBaseUtil.unionFieldToEnum(damselPayoutType, PayoutType.class));
        if (damselPayoutType.isSetWallet()) {
            payout.setWalletId(damselPayoutType.getWallet().getWalletId());
        }
        if (damselPayout.isSetSummary()) {
            List<PayoutSummaryItem> payoutSummaryItems = damselPayout.getSummary().stream()
                    .filter(payoutSummaryItem -> payoutSummaryItem.getOperationType() != OperationType.adjustment)
                    .collect(Collectors.toList());
            payout.setSummary(DamselUtil.toPayoutSummaryStatString(payoutSummaryItems));
        }
        return payout;
    }

    private void saveRussianPayoutAccount(
            PayoutAccount payoutAccount,
            Long extPayoutId
    ) {
        RussianPayoutAccount account = payoutAccount.getRussianPayoutAccount();
        RussianBankAccount bankAccount = account.getBankAccount();
        LegalAgreement legalAgreement = account.getLegalAgreement();

        var payoutAccountInfo = new com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount();
        payoutAccountInfo.setType(PayoutAccountType.russian_payout_account);
        payoutAccountInfo.setBankId(bankAccount.getAccount());
        payoutAccountInfo.setBankCorrId(bankAccount.getBankPostAccount());
        payoutAccountInfo.setBankLocalCode(bankAccount.getBankBik());
        payoutAccountInfo.setBankName(bankAccount.getBankName());
        payoutAccountInfo.setPurpose(account.getPurpose());
        payoutAccountInfo.setInn(account.getInn());
        payoutAccountInfo.setLegalAgreementId(legalAgreement.getLegalAgreementId());
        payoutAccountInfo.setLegalAgreementSignedAt(TypeUtil.stringToLocalDateTime(legalAgreement.getSignedAt()));
        payoutAccountInfo.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutAccountInfo(payoutAccountInfo);
    }

    private void saveInternationPayoutAccount(PayoutAccount payoutAccount, Long extPayoutId) {
        InternationalPayoutAccount account = payoutAccount.getInternationalPayoutAccount();
        InternationalLegalEntity legalEntity = account.getLegalEntity();
        InternationalBankAccount bankAccount = account.getBankAccount();
        LegalAgreement legalAgreement = account.getLegalAgreement();

        var payoutAccountInfo = new com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount();
        PayoutInternationalAccount internationalAccount = new PayoutInternationalAccount();

        payoutAccountInfo.setType(PayoutAccountType.international_payout_account);
        payoutAccountInfo.setTradingName(legalEntity.getTradingName());
        payoutAccountInfo.setLegalName(legalEntity.getLegalName());
        payoutAccountInfo.setActualAddress(legalEntity.getActualAddress());
        payoutAccountInfo.setRegisteredAddress(legalEntity.getRegisteredAddress());
        payoutAccountInfo.setRegisteredNumber(legalEntity.getRegisteredNumber());
        payoutAccountInfo.setPurpose(account.getPurpose());
        payoutAccountInfo.setBankId(bankAccount.getAccountHolder());
        payoutAccountInfo.setBankIban(bankAccount.getIban());
        payoutAccountInfo.setBankNumber(bankAccount.getNumber());
        if (bankAccount.isSetBank()) {
            fillBankInfo(bankAccount.getBank(), payoutAccountInfo);
        }
        if (bankAccount.isSetCorrespondentAccount()) {
            fillCorrespondentAccount(bankAccount.getCorrespondentAccount(), internationalAccount);
        }
        payoutAccountInfo.setLegalAgreementId(legalAgreement.getLegalAgreementId());
        payoutAccountInfo.setLegalAgreementSignedAt(TypeUtil.stringToLocalDateTime(legalAgreement.getSignedAt()));
        payoutAccountInfo.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutAccountInfo(payoutAccountInfo);

        internationalAccount.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutInternationalAccountInfo(internationalAccount);
    }

    private void fillCorrespondentAccount(InternationalBankAccount correspondentAccount,
                                          PayoutInternationalAccount internationalAccount) {
        internationalAccount.setBankAccount(correspondentAccount.getAccountHolder());
        internationalAccount.setBankNumber(correspondentAccount.getNumber());
        internationalAccount.setBankIban(correspondentAccount.getIban());
        if (correspondentAccount.isSetBank()) {
            InternationalBankDetails corrBankDetails = correspondentAccount.getBank();

            internationalAccount.setBankName(corrBankDetails.getName());
            internationalAccount.setBankAddress(corrBankDetails.getAddress());
            internationalAccount.setBankBic(corrBankDetails.getBic());
            internationalAccount.setBankAbaRtn(corrBankDetails.getAbaRtn());
            if (corrBankDetails.isSetCountry()) {
                String country = corrBankDetails.getCountry().toString();

                internationalAccount.setBankCountryCode(country);
            }
        }
    }

    private void fillBankInfo(InternationalBankDetails bankDetails,
                              com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount payout) {
        payout.setBankName(bankDetails.getName());
        payout.setBankAddress(bankDetails.getAddress());
        payout.setBankBic(bankDetails.getBic());
        payout.setBankAbaRtn(bankDetails.getAbaRtn());
        if (bankDetails.isSetCountry()) {
            String country = bankDetails.getCountry().toString();

            payout.setBankCountryCode(country);
        }
    }

    private PayoutState getPayoutState(Event event,
                                       com.rbkmoney.damsel.payout_processing.Payout damselPayout,
                                       String payoutId) {
        PayoutState payoutState = new PayoutState();
        payoutState.setEventId(event.getId());
        payoutState.setEventCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        payoutState.setPayoutId(payoutId);
        payoutState.setStatus(TBaseUtil.unionFieldToEnum(damselPayout.getStatus(), PayoutStatus.class));
        return payoutState;
    }
}

