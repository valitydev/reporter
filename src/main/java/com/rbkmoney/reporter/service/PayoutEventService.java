package com.rbkmoney.reporter.service;

import java.util.Optional;

public interface PayoutEventService {

    Optional<Long> getPayoutLastEventId();

}
