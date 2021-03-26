package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.damsel.domain.Representative;
import com.rbkmoney.reporter.exception.NotFoundException;
import com.rbkmoney.reporter.exception.ScheduleProcessingException;
import com.rbkmoney.reporter.exception.StorageException;

public interface TaskService {

    void registerProvisionOfServiceJob(String partyId, String contractId, long lastEventId,
                                       BusinessScheduleRef scheduleRef, Representative signer)
            throws ScheduleProcessingException, NotFoundException, StorageException;

    void deregisterProvisionOfServiceJob(String partyId, String contractId)
            throws ScheduleProcessingException, StorageException;

}
