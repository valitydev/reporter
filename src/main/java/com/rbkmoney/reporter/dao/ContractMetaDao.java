package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;

public interface ContractMetaDao extends GenericDao {

    Long getLastEventId() throws DaoException;

    void save(ContractMeta contractMeta) throws DaoException;

    ContractMeta get(String partyId, String contractId, ReportType reportType) throws DaoException;

    ContractMeta getExclusive(String partyId, String contractId, ReportType reportType) throws DaoException;

    List<ContractMeta> getByCalendarAndSchedulerId(int calendarId, int schedulerId) throws DaoException;

    List<ContractMeta> getAllActiveContracts() throws DaoException;

    void saveLastClosingBalance(String partyId, String contractId, ReportType reportType, long lastClosingBalance) throws DaoException;

    void disableContract(String partyId, String contractId, ReportType reportType) throws DaoException;

    void updateLastReportCreatedAt(String partyId, String contractId, ReportType reportType, LocalDateTime reportCreatedAt) throws DaoException;

}
