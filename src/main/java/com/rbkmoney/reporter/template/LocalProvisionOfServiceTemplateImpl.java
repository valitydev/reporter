package com.rbkmoney.reporter.template;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.reporter.dao.ContractMetaDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.exception.NotFoundException;
import com.rbkmoney.reporter.exception.StorageException;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.LocalStatisticService;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.util.FormatUtil;
import com.rbkmoney.reporter.util.TimeUtil;
import org.jxls.common.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
public class LocalProvisionOfServiceTemplateImpl implements ReportTemplate {

    public static final String DEFAULT_CURRENCY_CODE = "RUB";

    private final LocalStatisticService localStatisticService;

    private final PartyService partyService;

    private final ContractMetaDao contractMetaDao;

    private final Resource resource;

    @Autowired
    public LocalProvisionOfServiceTemplateImpl(
            LocalStatisticService localStatisticService,
            PartyService partyService,
            ContractMetaDao contractMetaDao,
            @Value("${report.type.pos.path|classpath:templates/provision_of_service_act.xlsx}") ClassPathResource resource
    ) {
        this.localStatisticService = localStatisticService;
        this.partyService = partyService;
        this.contractMetaDao = contractMetaDao;
        this.resource = resource;
    }

    @Override
    public boolean isAccept(ReportType reportType) {
        return reportType == ReportType.local_provision_of_service;
    }

    @Override
    public void processReportTemplate(Report report, OutputStream outputStream) throws IOException {
        try {
            String partyId = report.getPartyId();
            String shopId = report.getPartyShopId();
            LocalDateTime fromTime = report.getFromTime();
            LocalDateTime toTime = report.getToTime();
            Party party = partyService.getParty(partyId);
            Shop shop = getShop(party, shopId, partyId);

            String contractId = shop.getContractId();
            ContractMeta contractMeta = getContractMeta(contractId, partyId);
            contractMetaDao.updateLastReportCreatedAt(partyId, contractId, report.getCreatedAt());

            Context context = new Context();
            ZoneId reportZoneId = ZoneId.of(report.getTimezone());
            context.putVar("party_id", partyId);
            context.putVar("shop_id", shopId);
            context.putVar("contract_id", contractMeta.getContractId());
            context.putVar("created_at",
                    TimeUtil.toLocalizedDate(report.getCreatedAt().toInstant(ZoneOffset.UTC), reportZoneId));
            context.putVar("from_time",
                    TimeUtil.toLocalizedDate(fromTime.toInstant(ZoneOffset.UTC), reportZoneId));
            context.putVar("to_time",
                    TimeUtil.toLocalizedDate(toTime.minusNanos(1).toInstant(ZoneOffset.UTC), reportZoneId));

            Contract contract = getContract(party, contractId, partyId);

            if (contract.isSetLegalAgreement()) {
                LegalAgreement legalAgreement = contract.getLegalAgreement();
                context.putVar("legal_agreement_id", legalAgreement.getLegalAgreementId());
                context.putVar("legal_agreement_signed_at",
                        TimeUtil.toLocalizedDate(legalAgreement.getSignedAt(), reportZoneId));
            }

            if (contract.isSetContractor()
                    && contract.getContractor().isSetLegalEntity()
                    && contract.getContractor().getLegalEntity().isSetRussianLegalEntity()) {
                RussianLegalEntity entity = contract.getContractor()
                        .getLegalEntity()
                        .getRussianLegalEntity();
                context.putVar("registered_name", entity.getRegisteredName());
            }

            context.putVar("representative_full_name", contractMeta.getRepresentativeFullName());
            context.putVar("representative_position", contractMeta.getRepresentativePosition());

            ShopAccountingModel shopAccountingModel =
                    localStatisticService.getShopAccounting(partyId, shopId, DEFAULT_CURRENCY_CODE, fromTime, toTime);
            context.putVar("funds_acquired", FormatUtil.formatCurrency(shopAccountingModel.getFundsAcquired()));
            context.putVar("fee_charged", FormatUtil.formatCurrency(shopAccountingModel.getFeeCharged()));
            context.putVar("funds_paid_out", FormatUtil.formatCurrency(shopAccountingModel.getFundsPaidOut()));
            context.putVar("funds_refunded", FormatUtil.formatCurrency(shopAccountingModel.getFundsRefunded()));

            long openingBalance;
            if (contractMeta.getLastClosingBalance() == null) {
                ShopAccountingModel previousPeriod =
                        localStatisticService.getShopAccounting(partyId, shopId, DEFAULT_CURRENCY_CODE, fromTime);
                openingBalance = previousPeriod.getAvailableFunds();
            } else {
                openingBalance = contractMeta.getLastClosingBalance();
            }
            long closingBalance = openingBalance + shopAccountingModel.getAvailableFunds();
            //contractMetaDao.saveLastClosingBalance(report.getPartyId(), contractId, closingBalance);
            context.putVar("opening_balance", FormatUtil.formatCurrency(openingBalance));
            context.putVar("closing_balance", FormatUtil.formatCurrency(closingBalance));

            processTemplate(context, resource.getInputStream(), outputStream);
        } catch (DaoException ex) {
            throw new StorageException(ex);
        }
    }

    private Shop getShop(Party party, String shopId, String partyId) {
        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Failed to find shop for provision of service report " +
                    "(partyId='%s', shopId='%s')", partyId, shopId));
        }
        return shop;
    }

    private ContractMeta getContractMeta(String contractId, String partyId) throws DaoException {
        ContractMeta contractMeta = contractMetaDao.getExclusive(partyId, contractId);
        if (contractMeta == null) {
            throw new NotFoundException(String.format("Failed to find meta data for provision of service report " +
                    "(partyId='%s', contractId='%s')", partyId, contractId));
        }
        return contractMeta;
    }

    private Contract getContract(Party party, String contractId, String partyId) {
        Contract contract = party.getContracts().get(contractId);
        if (contract == null) {
            throw new NotFoundException(String.format("Failed to find contract for provision of service report" +
                    "(partyId='%s', contractId='%s')", partyId, contractId));
        }
        return contract;
    }

}
