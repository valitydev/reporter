package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.config.AbstractHandlerConfig;
import com.rbkmoney.reporter.handler.EventHandler;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.TaskService;
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
                PartyManagementEventHandlerImpl.class},
        initializers = AbstractHandlerConfig.Initializer.class
)
public class PartyManagementHandlersTest extends AbstractHandlerConfig {

    private static final  String CONTRACT_EFFECT_CREATED = "created";
    private static final  String CONTRACT_EFFECT_REPORT_PREFERENCES = "report_preferences";
    @Autowired
    private EventHandler<PartyChange> partyManagementEventHandler;
    @MockBean
    private TaskService taskService;

    private static KafkaEvent createTestKafkaEvent() {
        return new KafkaEvent("test", 1, 1, createTestMachineEvent());
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
        ClaimStatusChanged claimStatusChanged = new ClaimStatusChanged();
        claimStatusChanged.setId(random(Long.class));
        List<ClaimEffect> claimEffectList = new ArrayList<>();
        // there must be 2 successful operations
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_CREATED, true, true));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_CREATED, true, false));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_CREATED, false, false));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_REPORT_PREFERENCES, false, true));
        claimEffectList.add(createTestClaimEffect(CONTRACT_EFFECT_REPORT_PREFERENCES, false, false));
        claimEffectList.add(createTestClaimEffect(Strings.EMPTY, false, false));
        ClaimAccepted accepted = new ClaimAccepted();
        accepted.setEffects(claimEffectList);
        ClaimStatus status = new ClaimStatus();
        status.setAccepted(accepted);
        claimStatusChanged.setStatus(status);
        PartyChange partyChange = new PartyChange();
        partyChange.setClaimStatusChanged(claimStatusChanged);
        return partyChange;
    }

    private static ClaimEffect createTestClaimEffect(String contractorEffect,
                                                     boolean isSetReport,
                                                     boolean isSetCreatedAct) {
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
        ClaimEffect claimEffect = new ClaimEffect();
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

    @Test
    public void partyManagementEventHandlerTest() throws Exception {
        PartyEventData partyEventData = createTestPartyEventData();
        for (PartyChange change : partyEventData.getChanges()) {
            partyManagementEventHandler.handle(createTestKafkaEvent(), change, 0);
        }
        verify(taskService, times(2))
                .registerProvisionOfServiceJob(
                        anyString(), // party id
                        anyString(), // contract id
                        anyLong(),   // event id
                        any(BusinessScheduleRef.class),
                        any(Representative.class)
                );
    }

}

