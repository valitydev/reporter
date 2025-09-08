package dev.vality.reporter.service.impl;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.domain_config_v2.*;
import dev.vality.reporter.exception.DominantException;
import dev.vality.reporter.exception.NotFoundException;
import dev.vality.reporter.service.DominantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DominantServiceImpl implements DominantService {

    private final RepositoryClientSrv.Iface dominantClient;

    @Override
    @Cacheable(value = "currencies", cacheManager = "currenciesCacheManager")
    public Currency getCurrency(String symbolicCode) {
        log.debug("Trying to get currency, symbolicCode='{}'", symbolicCode);
        Reference revisionReference = new Reference();
        CurrencyRef currencyRef = new CurrencyRef();
        currencyRef.setSymbolicCode(symbolicCode);
        revisionReference.setCurrency(currencyRef);
        VersionedObject versionedObject = getVersionedObject(revisionReference);
        Currency currency = versionedObject.getObject().getCurrency().getData();
        log.debug("Currency has been found, currencyRef='{}', revisionReference='{}', currency='{}'",
                currencyRef, revisionReference, currency);
        return currency;
    }

    private VersionedObject getVersionedObject(Reference reference) {
        VersionReference versionRef = new VersionReference();
        versionRef.setHead(new Head());
        try {
            return dominantClient.checkoutObject(versionRef, reference);
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, objectRef='%s', versionRef='%s'",
                    reference, versionRef), ex);
        } catch (TException ex) {
            throw new DominantException(String.format("Failed to get object, objectRef='%s', " +
                    "versionRef='%s'", reference, versionRef), ex);
        }
    }

    @Override
    public Map<String, ShopConfig> getShopConfigs(String partyId) {
        var versionedObjectWithReferences = getPartyObject(partyId);
        var shopConfigObjects = versionedObjectWithReferences.getReferencedBy().stream()
                .map(VersionedObject::getObject)
                .map(DomainObject::getShopConfig)
                .toList();
        log.debug("Receive shops for partyId: {}, shopConfigObjects ='{}'", partyId, shopConfigObjects);
        return shopConfigObjects.stream()
                .collect(Collectors.toMap(
                        object -> object.getRef().getId(),
                        ShopConfigObject::getData));
    }

    private VersionedObjectWithReferences getPartyObject(String partyId) {
        log.debug("Trying to get party, partyId='{}'", partyId);
        Reference reference = new Reference();
        PartyConfigRef partyConfigRef = new PartyConfigRef();
        partyConfigRef.setId(partyId);
        reference.setPartyConfig(partyConfigRef);
        var partyObject = getVersionedObjectWithReferences(reference);
        log.debug("Party has been found, partyConfig ='{}', partyConfigRef='{}'",
                partyObject.getObject().getObject().getPartyConfig(), partyConfigRef);
        return partyObject;
    }

    private VersionedObjectWithReferences getVersionedObjectWithReferences(Reference reference) {
        VersionReference versionRef = new VersionReference();
        versionRef.setHead(new Head());
        try {
            return dominantClient.checkoutObjectWithReferences(versionRef, reference);
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, objectRef='%s', versionRef='%s'",
                    reference, versionRef), ex);
        } catch (TException ex) {
            throw new DominantException(String.format("Failed to get object with references, objectRef='%s', " +
                    "versionRef='%s'", reference, versionRef), ex);
        }
    }
}
