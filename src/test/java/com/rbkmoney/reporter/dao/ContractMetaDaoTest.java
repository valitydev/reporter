package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

public class ContractMetaDaoTest extends AbstractIntegrationTest {

    @Autowired
    private ContractMetaDao contractMetaDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        String partyId = "test";
        String contractId = "test";

        ContractMeta contractMeta = random(ContractMeta.class, "partyId", "contractId", "reportType");
        contractMeta.setPartyId(partyId);
        contractMeta.setContractId(contractId);

        contractMetaDao.save(contractMeta);
        ContractMeta contractMeta2 = contractMetaDao.get(contractMeta.getPartyId(), contractMeta.getContractId());
        assertEquals(contractMeta.getPartyId(), contractMeta2.getPartyId());
        assertEquals(contractMeta.getContractId(), contractMeta2.getContractId());
        assertEquals(contractMeta.getScheduleId(), contractMeta2.getScheduleId());
        assertEquals(contractMeta.getLastEventId(), contractMeta2.getLastEventId());
        assertEquals(contractMeta.getCalendarId(), contractMeta2.getCalendarId());

        assertEquals(contractMeta.getLastEventId(), contractMetaDao.getLastEventId());

        assertEquals(contractMeta2, contractMetaDao.getAllActiveContracts().get(0));

        contractMeta = random(ContractMeta.class, "partyId", "contractId", "reportType");
        contractMeta.setPartyId(partyId);
        contractMeta.setContractId(contractId);

        contractMetaDao.save(contractMeta);
        contractMeta2 = contractMetaDao.get(contractMeta.getPartyId(), contractMeta.getContractId());
        assertEquals(contractMeta.getPartyId(), contractMeta2.getPartyId());
        assertEquals(contractMeta.getContractId(), contractMeta2.getContractId());
        assertEquals(contractMeta.getScheduleId(), contractMeta2.getScheduleId());
        assertEquals(contractMeta.getLastEventId(), contractMeta2.getLastEventId());
        assertEquals(contractMeta.getCalendarId(), contractMeta2.getCalendarId());
    }

}
