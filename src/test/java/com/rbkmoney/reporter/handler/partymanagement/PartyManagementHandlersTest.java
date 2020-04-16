package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.config.AbstractHandlerConfig;
import com.rbkmoney.reporter.service.TaskService;
import com.rbkmoney.reporter.service.impl.S3StorageServiceImpl;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ContextConfiguration(
        classes = {
                ContractReportPreferencesHandler.class,
                ContractCreatedChangesHandler.class,
                ContractEffectHander.class,
                PartyManagementEventHandler.class},
        initializers = AbstractHandlerConfig.Initializer.class
)
public class PartyManagementHandlersTest extends AbstractHandlerConfig {

    @Autowired
    private EventHandler<PartyEventData> partyManagementEventHandler;

    @MockBean
    private TaskService taskService;

    @MockBean
    private S3StorageServiceImpl s3StorageService;

    private final static String CONTRACT_EFFECT_CREATED = "created";

    private final static String CONTRACT_EFFECT_REPORT_PREFERENCES = "report_preferences";

    @Test
    public void partyManagementEventHandlerTest() {
        MachineEvent machineEvent = createTestMachineEvent();
        PartyEventData partyEventData = createTestPartyEventData();
        partyManagementEventHandler.handle(machineEvent, partyEventData);
        verify(taskService, times(2))
                .registerProvisionOfServiceJob(
                        anyString(), // party id
                        anyString(), // contract id
                        anyLong(),   // event id
                        any(BusinessScheduleRef.class),
                        any(Representative.class)
                );
    }

    private static MachineEvent createTestMachineEvent() {
        MachineEvent event = new MachineEvent();
        event.setEventId(random(Long.class));
        event.setSourceId(random(String.class));
        return event;
    }

    private static PartyEventData createTestPartyEventData() {
        PartyEventData eventData = new PartyEventData();
        eventData.setChanges(createTestPartyChangesList(1));
        return eventData;
    }

    private static List<PartyChange> createTestPartyChangesList(int count) {
        List<PartyChange> partyChanges = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            partyChanges.add(createTestPartyChange());
        }
        return partyChanges;
    }

    private static PartyChange createTestPartyChange() {
        PartyChange partyChange = new PartyChange();
        ClaimStatusChanged claimStatusChanged = new ClaimStatusChanged();
        claimStatusChanged.setId(random(Long.class));
        ClaimStatus status = new ClaimStatus();
        ClaimAccepted accepted = new ClaimAccepted();
        List<ClaimEffect> claimEffectList = new ArrayList<>();
        // there must be 2 successful operations
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_CREATED, true, true));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_CREATED, true, false));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_CREATED, false, false));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_REPORT_PREFERENCES, false, true));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_REPORT_PREFERENCES, false, false));
        claimEffectList.add(createTestClaimEffect(Strings.EMPTY, false, false));
        accepted.setEffects(claimEffectList);
        status.setAccepted(accepted);
        claimStatusChanged.setStatus(status);
        partyChange.setClaimStatusChanged(claimStatusChanged);
        return partyChange;
    }

    private static ClaimEffect createTestClaimEffect(String contractorEffect,
                                                     boolean isSetReport,
                                                     boolean isSetCreatedAct) {
        ClaimEffect claimEffect = new ClaimEffect();
        ContractEffectUnit contractEffectUnit = new ContractEffectUnit();
        contractEffectUnit.setContractId(random(String.class));
        ContractEffect contractEffect = new ContractEffect();
        switch (contractorEffect) {
            case CONTRACT_EFFECT_CREATED:
                contractEffect.setCreated(createTestContractCreated(isSetReport, isSetCreatedAct));
                break;
            case CONTRACT_EFFECT_REPORT_PREFERENCES:
                contractEffect.setReportPreferencesChanged(createTestReportPreferences(isSetCreatedAct));
                break;
            default:
                contractEffect.setContractorChanged(Strings.EMPTY);
        }

        contractEffectUnit.setEffect(contractEffect);
        claimEffect.setContractEffect(contractEffectUnit);
        return claimEffect;
    }

    private static Contract createTestContractCreated(boolean isSetReport, boolean isSetCreatedAct) {
        Contract contract = new Contract();
        if (isSetReport) {
            contract.setReportPreferences(createTestReportPreferences(isSetCreatedAct));
        } else {
            contract.setContractor(new Contractor());
        }
        return contract;
    }

    private static ReportPreferences createTestReportPreferences(boolean isSetCreatedAct) {
        ReportPreferences reportPreferences = new ReportPreferences();
        if (isSetCreatedAct) {
            ServiceAcceptanceActPreferences actPreferences = new ServiceAcceptanceActPreferences();
            actPreferences.setSchedule(new BusinessScheduleRef().setId(1));
            actPreferences.setSigner(new Representative()
                    .setFullName(random(String.class))
                    .setPosition(random(String.class))
                    .setDocument(new RepresentativeDocument())
            );
            reportPreferences.setServiceAcceptanceActPreferences(actPreferences);
        }
        return reportPreferences;
    }

}

