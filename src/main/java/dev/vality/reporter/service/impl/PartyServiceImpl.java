package dev.vality.reporter.service.impl;

import dev.vality.damsel.domain.Contract;
import dev.vality.damsel.domain.Party;
import dev.vality.damsel.domain.PaymentInstitutionRef;
import dev.vality.damsel.domain.Shop;
import dev.vality.damsel.msgpack.Value;
import dev.vality.damsel.payment_processing.*;
import dev.vality.reporter.exception.ContractNotFoundException;
import dev.vality.reporter.exception.NotFoundException;
import dev.vality.reporter.exception.PartyNotFoundException;
import dev.vality.reporter.exception.ShopNotFoundException;
import dev.vality.reporter.service.PartyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PartyServiceImpl implements PartyService {

    private final PartyManagementSrv.Iface partyManagementClient;

    @Autowired
    public PartyServiceImpl(PartyManagementSrv.Iface partyManagementClient) {
        this.partyManagementClient = partyManagementClient;
    }

    @Override
    public Party getParty(String partyId) throws PartyNotFoundException {
        return getParty(partyId, getPartyRevision(partyId));
    }

    @Override
    public Party getParty(String partyId, long partyRevision) throws PartyNotFoundException {
        return getParty(partyId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Party getParty(String partyId, PartyRevisionParam partyRevisionParam) throws PartyNotFoundException {
        log.info("Trying to get party, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
        try {
            Party party = partyManagementClient.checkout(partyId, partyRevisionParam);
            log.info("Party has been found, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
            return party;
        } catch (PartyNotFound ex) {
            throw new PartyNotFoundException(
                    String.format("Party not found, partyId='%s', partyRevisionParam='%s'", partyId,
                            partyRevisionParam), ex
            );
        } catch (InvalidPartyRevision ex) {
            throw new PartyNotFoundException(
                    String.format("Invalid party revision, partyId='%s', partyRevisionParam='%s'", partyId,
                            partyRevisionParam), ex
            );
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get party, partyId='%s', partyRevisionParam='%s'", partyId,
                            partyRevisionParam), ex
            );
        }
    }

    @Override
    public Shop getShop(String partyId, String shopId) throws ShopNotFoundException, PartyNotFoundException {
        log.info("Trying to get shop, partyId='{}', shopId='{}'", partyId, shopId);
        Party party = getParty(partyId);
        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new ShopNotFoundException(
                    String.format("Shop not found, partyId='%s', shopId='%s'", partyId, shopId));
        }
        log.info("Shop has been found, partyId='{}', shopId='{}'", partyId, shopId);
        return shop;
    }

    @Override
    public Shop getShop(String partyId, String shopId, long partyRevision)
            throws ShopNotFoundException, PartyNotFoundException {
        return getShop(partyId, shopId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Shop getShop(String partyId, String shopId, PartyRevisionParam partyRevisionParam)
            throws ShopNotFoundException, PartyNotFoundException {
        log.info("Trying to get shop, partyId='{}', shopId='{}', partyRevisionParam='{}'", partyId, shopId,
                partyRevisionParam);
        Party party = getParty(partyId, partyRevisionParam);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new ShopNotFoundException(
                    String.format("Shop not found, partyId='%s', shopId='%s', partyRevisionParam='%s'", partyId, shopId,
                            partyRevisionParam));
        }
        log.info("Shop has been found, partyId='{}', shopId='{}', partyRevisionParam='{}'", partyId, shopId,
                partyRevisionParam);
        return shop;
    }

    @Override
    public long getPartyRevision(String partyId) {
        try {
            log.info("Trying to get revision, partyId='{}'", partyId);
            long revision = partyManagementClient.getRevision(partyId);
            log.info("Revision has been found, partyId='{}', revision='{}'", partyId, revision);
            return revision;
        } catch (PartyNotFound ex) {
            throw new PartyNotFoundException(String.format("Party not found, partyId='%s'", partyId), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get party revision, partyId='%s'", partyId), ex);
        }
    }

    @Override
    public Contract getContract(String partyId, String contractId)
            throws ContractNotFoundException, PartyNotFoundException {
        log.info("Trying to get contract, partyId='{}', contractId='{}'", partyId, contractId);
        Party party = getParty(partyId);
        Contract contract = party.getContracts().get(contractId);
        if (contract == null) {
            throw new ContractNotFoundException(
                    String.format("Contract not found, partyId='%s', contractId='%s'", partyId, contractId));
        }
        log.info("Contract has been found, partyId='{}', contractId='{}'", partyId, contractId);
        return contract;
    }

    @Override
    public Contract getContract(String partyId, String contractId, long partyRevision)
            throws ContractNotFoundException, PartyNotFoundException {
        return getContract(partyId, contractId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Contract getContract(String partyId, String contractId, PartyRevisionParam partyRevisionParam)
            throws ContractNotFoundException, PartyNotFoundException {
        log.info("Trying to get contract, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId,
                partyRevisionParam);
        Party party = getParty(partyId, partyRevisionParam);

        Contract contract = party.getContracts().get(contractId);
        if (contract == null) {
            throw new ContractNotFoundException(
                    String.format("Contract not found, partyId='%s', contractId='%s', partyRevisionParam='%s'", partyId,
                            contractId, partyRevisionParam));
        }
        log.info("Contract has been found, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId,
                partyRevisionParam);
        return contract;
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId)
            throws ContractNotFoundException, PartyNotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, getPartyRevision(partyId));
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, long partyRevision)
            throws ContractNotFoundException, PartyNotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId,
                                                          PartyRevisionParam partyRevisionParam)
            throws ContractNotFoundException, PartyNotFoundException {
        log.debug("Trying to get paymentInstitutionRef, partyId='{}', contractId='{}', partyRevisionParam='{}'",
                partyId, contractId, partyRevisionParam);
        Contract contract = getContract(partyId, contractId, partyRevisionParam);

        if (!contract.isSetPaymentInstitution()) {
            throw new NotFoundException(String.format(
                    "PaymentInstitutionRef not found, partyId='%s', contractId='%s', partyRevisionParam='%s'", partyId,
                    contractId, partyRevisionParam));
        }

        PaymentInstitutionRef paymentInstitutionRef = contract.getPaymentInstitution();
        log.info("PaymentInstitutionRef has been found, partyId='{}', contractId='{}, " +
                        "paymentInstitutionRef='{}', partyRevisionParam='{}'",
                partyId, contractId, paymentInstitutionRef, partyRevisionParam);
        return paymentInstitutionRef;
    }

    @Override
    public String getShopUrl(String partyId, String shopId) throws PartyNotFoundException, ShopNotFoundException {
        Shop shop = getShop(partyId, shopId);
        if (shop.getLocation().isSetUrl()) {
            return shop.getLocation().getUrl();
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> getShopUrls(String partyId) throws PartyNotFoundException {
        Party party = getParty(partyId);
        return party.getShops().values().stream()
                .collect(Collectors.toMap(Shop::getId, shop -> {
                    if (shop.getLocation().isSetUrl()) {
                        return shop.getLocation().getUrl();
                    } else {
                        return null;
                    }
                }));
    }

    @Override
    public Value getMetaData(String partyId, String namespace) throws NotFoundException {
        try {
            return partyManagementClient.getMetaData(partyId, namespace);
        } catch (PartyMetaNamespaceNotFound ex) {
            return null;
        } catch (PartyNotFound ex) {
            throw new NotFoundException(
                    String.format("Party not found, partyId='%s', namespace='%s'", partyId, namespace),
                    ex
            );
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get namespace, partyId='%s', namespace='%s'", partyId, namespace), ex
            );
        }
    }

}
