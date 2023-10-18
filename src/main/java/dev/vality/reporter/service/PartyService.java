package dev.vality.reporter.service;

import dev.vality.damsel.domain.Contract;
import dev.vality.damsel.domain.Party;
import dev.vality.damsel.domain.PaymentInstitutionRef;
import dev.vality.damsel.domain.Shop;
import dev.vality.damsel.msgpack.Value;
import dev.vality.damsel.payment_processing.PartyRevisionParam;
import dev.vality.reporter.exception.ContractNotFoundException;
import dev.vality.reporter.exception.NotFoundException;
import dev.vality.reporter.exception.PartyNotFoundException;
import dev.vality.reporter.exception.ShopNotFoundException;

import java.util.Map;

public interface PartyService {

    Party getParty(String partyId) throws PartyNotFoundException;

    Party getParty(String partyId, long partyRevision) throws PartyNotFoundException;

    Party getParty(String partyId, PartyRevisionParam partyRevisionParam) throws PartyNotFoundException;

    Shop getShop(String partyId, String shopId) throws ShopNotFoundException, PartyNotFoundException;

    Shop getShop(String partyId, String shopId, long partyRevision)
            throws ShopNotFoundException, PartyNotFoundException;

    Shop getShop(String partyId, String shopId, PartyRevisionParam partyRevisionParam)
            throws ShopNotFoundException, PartyNotFoundException;

    long getPartyRevision(String partyId);

    Contract getContract(String partyId, String contractId) throws ContractNotFoundException, PartyNotFoundException;

    Contract getContract(String partyId, String contractId, long partyRevision)
            throws ContractNotFoundException, PartyNotFoundException;

    Contract getContract(String partyId, String contractId, PartyRevisionParam partyRevisionParam)
            throws ContractNotFoundException, PartyNotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId)
            throws ContractNotFoundException, PartyNotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, long partyRevision)
            throws ContractNotFoundException, PartyNotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId,
                                                   PartyRevisionParam partyRevisionParam)
            throws ContractNotFoundException, PartyNotFoundException;

    String getShopUrl(String partyId, String shopId) throws PartyNotFoundException, ShopNotFoundException;

    Map<String, String> getShopUrls(String partyId) throws PartyNotFoundException;

    Map<String, String> getShopNames(String partyId) throws PartyNotFoundException;

    Value getMetaData(String partyId, String namespace) throws NotFoundException;

}
