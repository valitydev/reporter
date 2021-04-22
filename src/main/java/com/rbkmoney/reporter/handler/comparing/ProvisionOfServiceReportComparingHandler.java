package com.rbkmoney.reporter.handler.comparing;

import com.rbkmoney.reporter.dao.ReportComparingDataDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.LocalStatisticService;
import com.rbkmoney.reporter.service.StatisticService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.rbkmoney.reporter.template.LocalProvisionOfServiceTemplateImpl.DEFAULT_CURRENCY_CODE;

@Slf4j
@Component
public class ProvisionOfServiceReportComparingHandler extends ReportComparingAbstractHandler {

    private final StatisticService statisticService;
    private final LocalStatisticService localStatisticService;

    public ProvisionOfServiceReportComparingHandler(ReportComparingDataDao reportComparingDataDao,
                                                    StatisticService statisticService,
                                                    LocalStatisticService localStatisticService) {
        super(reportComparingDataDao);
        this.statisticService = statisticService;
        this.localStatisticService = localStatisticService;
    }

    @Override
    public void compareReport(Report report) {
        if (checkProvisionOfServiceFunds(report)) {
            return;
        }
        if (checkProvisionOfServiceBalances(report)) {
            return;
        }
        saveSuccessComparingInfo(report.getId(), report.getType());
    }

    private boolean checkProvisionOfServiceFunds(Report report) {
        Long reportId = report.getId();
        String partyId = report.getPartyId();
        String shopId = report.getPartyShopId();
        LocalDateTime fromTime = report.getFromTime();
        LocalDateTime toTime = report.getToTime();
        log.info("Start checking ProvisionOfService funds (reportId: {}, partyId: {}, shopId: {}, " +
                "fromTime: {}, toTime:{})", reportId, partyId, shopId, fromTime, toTime);

        ShopAccountingModel magistaModel = statisticService.getShopAccounting(
                partyId,
                shopId,
                DEFAULT_CURRENCY_CODE,
                fromTime.toInstant(ZoneOffset.UTC),
                toTime.toInstant(ZoneOffset.UTC)
        );

        ShopAccountingModel localModel = localStatisticService.getShopAccounting(
                partyId,
                shopId,
                DEFAULT_CURRENCY_CODE,
                fromTime,
                toTime
        );
        log.info("Received models for report {}: magista - {}, local - {}", reportId, magistaModel, localModel);

        if (checkProvisionOfServiceModels(magistaModel, localModel, reportId, Strings.EMPTY)) {
            return true;
        }

        return false;
    }

    private boolean checkProvisionOfServiceBalances(Report report) {
        Long reportId = report.getId();
        String partyId = report.getPartyId();
        String shopId = report.getPartyShopId();
        LocalDateTime fromTime = report.getFromTime();
        log.info("Start checking ProvisionOfService balances (reportId: {}, partyId: {}, shopId: {}, " +
                "fromTime: {})", reportId, partyId, shopId, fromTime);

        ShopAccountingModel magistaModel = statisticService.getShopAccounting(
                partyId,
                shopId,
                DEFAULT_CURRENCY_CODE,
                fromTime.toInstant(ZoneOffset.UTC)
        );

        ShopAccountingModel localModel =
                localStatisticService.getShopAccounting(partyId, shopId, DEFAULT_CURRENCY_CODE, fromTime);

        log.info("Received models for report {} (balances): magista - {}, local - {}",
                reportId, magistaModel, localModel);

        if (checkProvisionOfServiceModels(magistaModel, localModel, reportId, "(balances)")) {
            return true;
        }

        return false;
    }

    private boolean checkProvisionOfServiceModels(ShopAccountingModel magistaModel,
                                                  ShopAccountingModel localModel,
                                                  long reportId,
                                                  String modelType) {
        if (checkEmptyModel(magistaModel, reportId, "Magista " + modelType)
                || checkEmptyModel(localModel, reportId, "Local " + modelType)
                || checkModelsEqual(magistaModel, localModel, reportId, modelType)) {
            return true;
        }

        return false;
    }

    private boolean checkModelsEqual(ShopAccountingModel magistaModel,
                                     ShopAccountingModel localModel,
                                     long reportId,
                                     String modelType) {
        if (magistaModel.equals(localModel)) {
            return false;
        }

        String failureReason = String.format("Models for report %s %s are not equal! (magista: %s, local: %s)",
                reportId, modelType, magistaModel, localModel);
        saveErrorComparingInfo(failureReason, reportId, ReportType.provision_of_service);
        return true;
    }

    private boolean checkEmptyModel(Object model, long reportId, String type) {
        if (model != null) {
            return false;
        }

        String failureReason = String.format("%s shop accounting model for " +
                "report %s is not found!", type, reportId);
        saveErrorComparingInfo(failureReason, reportId, ReportType.provision_of_service);
        return true;
    }
}
