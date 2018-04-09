package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.tables.pojos.PosReportMeta;
import com.rbkmoney.reporter.exception.DaoException;

import java.time.LocalDateTime;

public interface PosReportMetaDao extends GenericDao {

    PosReportMeta get(String partyId, String contractId) throws DaoException;

    void updateLastBalanceAndCreatedAt(String partyId, String contractId, long openingBalance, long closingBalance, LocalDateTime reportCreatedAt) throws DaoException;

}
