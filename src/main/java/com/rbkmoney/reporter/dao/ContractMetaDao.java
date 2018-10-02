package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;

public interface ContractMetaDao extends GenericDao {

    Long getLastEventId() throws DaoException;

    void save(ContractMeta contractMeta) throws DaoException;

    ContractMeta get(String partyId, String contractId) throws DaoException;

    ContractMeta getExclusive(String partyId, String contractId) throws DaoException;

    List<ContractMeta> getByCalendarAndSchedulerId(int calendarId, int schedulerId) throws DaoException;

    List<ContractMeta> getAllActiveContracts() throws DaoException;

    void saveLastClosingBalance(String partyId, String contractId, long lastClosingBalance) throws DaoException;

    void disableContract(String partyId, String contractId) throws DaoException;

    void updateLastReportCreatedAt(String partyId, String contractId, LocalDateTime reportCreatedAt) throws DaoException;

}
