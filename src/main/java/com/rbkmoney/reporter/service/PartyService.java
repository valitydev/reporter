package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.exception.PartyNotFoundException;
import com.rbkmoney.reporter.exception.ShopNotFoundException;
import com.rbkmoney.reporter.model.PartyModel;

import java.time.Instant;

/**
 * Created by tolkonepiu on 17/07/2017.
 */
public interface PartyService {

    PartyModel getPartyRepresentation(String partyId, String shopId, Instant timestamp) throws PartyNotFoundException, ShopNotFoundException;

}
