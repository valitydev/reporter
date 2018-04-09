package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.damsel.domain.Contract;
import com.rbkmoney.damsel.domain.LegalAgreement;
import com.rbkmoney.damsel.domain.RussianLegalEntity;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.ReportType;
import com.rbkmoney.reporter.dao.PosReportMetaDao;
import com.rbkmoney.reporter.domain.tables.pojos.PosReportMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.exception.StorageException;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.StatisticService;
import com.rbkmoney.reporter.service.TemplateService;
import com.rbkmoney.reporter.util.FormatUtil;
import com.rbkmoney.reporter.util.TimeUtil;
import org.jxls.common.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
public class ProvisionOfServiceTemplateImpl implements TemplateService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String DEFAULT_REPORT_CURRENCY_CODE = "RUB";

    private final StatisticService statisticService;

    private final PartyService partyService;

    private final PosReportMetaDao posReportMetaDao;

    @Autowired
    public ProvisionOfServiceTemplateImpl(StatisticService statisticService, PartyService partyService, PosReportMetaDao posReportMetaDao) {
        this.statisticService = statisticService;
        this.partyService = partyService;
        this.posReportMetaDao = posReportMetaDao;
    }

    @Override
    public void processReportTemplate(Report report, OutputStream outputStream) throws IOException {
        try {
            Instant createdAt = report.getCreatedAt().toInstant(ZoneOffset.UTC);

            Context context = new Context();
            ZoneId reportZoneId = ZoneId.of(report.getTimezone());
            context.putVar("party_id", report.getPartyId());
            context.putVar("contract_id", report.getPartyContractId());
            context.putVar("created_at", TimeUtil.toLocalizedDate(report.getCreatedAt().toInstant(ZoneOffset.UTC), reportZoneId));
            context.putVar("from_time", TimeUtil.toLocalizedDate(report.getFromTime().toInstant(ZoneOffset.UTC), reportZoneId));
            context.putVar("to_time", TimeUtil.toLocalizedDate(report.getToTime().minusNanos(1).toInstant(ZoneOffset.UTC), reportZoneId));

            Contract contract = partyService.getContract(report.getPartyId(), report.getPartyContractId(), createdAt);
            if (contract.isSetLegalAgreement()) {
                LegalAgreement legalAgreement = contract.getLegalAgreement();
                context.putVar("legal_agreement_id", legalAgreement.getLegalAgreementId());
                context.putVar("legal_agreement_signed_at", TimeUtil.toLocalizedDate(legalAgreement.getSignedAt(), reportZoneId));
            }

            if (contract.isSetContractor()
                    && contract.getContractor().isSetLegalEntity()
                    && contract.getContractor().getLegalEntity().isSetRussianLegalEntity()) {
                RussianLegalEntity entity = contract.getContractor()
                        .getLegalEntity()
                        .getRussianLegalEntity();
                context.putVar("registered_name", entity.getRegisteredName());
                context.putVar("representative_full_name", entity.getRepresentativeFullName());
                context.putVar("representative_position", entity.getRepresentativePosition());
            }

            PosReportMeta posReportMeta = posReportMetaDao.get(report.getPartyId(), report.getPartyContractId());

            long openingBalance;
            if (posReportMeta != null && posReportMeta.getLastClosingBalance() != null) {
                log.info("Report meta has been found, reportMeta='{}'", posReportMeta);
                openingBalance = posReportMeta.getLastClosingBalance();
            } else {
                ShopAccountingModel previousPeriod = statisticService.getShopAccounting(
                        report.getPartyId(),
                        report.getPartyContractId(),
                        DEFAULT_REPORT_CURRENCY_CODE,
                        report.getFromTime().toInstant(ZoneOffset.UTC)
                );
                openingBalance = previousPeriod.getAvailableFunds();
            }
            context.putVar("opening_balance", FormatUtil.formatCurrency(openingBalance));

            ShopAccountingModel shopAccountingModel = statisticService.getShopAccounting(
                    report.getPartyId(),
                    report.getPartyContractId(),
                    DEFAULT_REPORT_CURRENCY_CODE,
                    report.getFromTime().toInstant(ZoneOffset.UTC),
                    report.getToTime().toInstant(ZoneOffset.UTC)
            );
            context.putVar("funds_acquired", FormatUtil.formatCurrency(shopAccountingModel.getFundsAcquired()));
            context.putVar("fee_charged", FormatUtil.formatCurrency(shopAccountingModel.getFeeCharged()));
            context.putVar("funds_paid_out", FormatUtil.formatCurrency(shopAccountingModel.getFundsPaidOut()));
            context.putVar("funds_refunded", FormatUtil.formatCurrency(shopAccountingModel.getFundsRefunded()));

            long closingBalance = openingBalance + shopAccountingModel.getAvailableFunds();
            context.putVar("closing_balance", FormatUtil.formatCurrency(closingBalance));

            processTemplate(context, ReportType.provision_of_service.getTemplateResource().getInputStream(), outputStream);

            posReportMetaDao.updateLastBalanceAndCreatedAt(report.getPartyId(), report.getPartyContractId(), openingBalance, closingBalance, report.getCreatedAt());
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get or save report meta, partyId='%s', contractId='%s'", report.getPartyId(), report.getPartyContractId()), ex);
        }
    }

    @Override
    public List<ReportType> getReportTypes() {
        return Arrays.asList(ReportType.provision_of_service);
    }
}
