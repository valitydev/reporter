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

import java.util.Collections;
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
        var versionedObject = getPartyObject(partyId);
        List<Reference> shopReferences = getShopReferences(versionedObject);
        List<VersionedObject> shopObjects = getVersionedObjects(shopReferences);
        return shopObjects.stream()
                .collect(Collectors.toMap(
                        object -> object.getObject().getShopConfig().getRef().getId(),
                        object -> object.getObject().getShopConfig().getData()));
    }

    private VersionedObject getPartyObject(String partyId) {
        Reference reference = new Reference();
        PartyConfigRef partyConfigRef = new PartyConfigRef();
        partyConfigRef.setId(partyId);
        reference.setPartyConfig(partyConfigRef);
        return getVersionedObject(reference);
    }

    private List<Reference> getShopReferences(VersionedObject versionedObject) {
        var partyConfig = versionedObject.getObject().getPartyConfig().getData();
        return partyConfig.getShops().stream()
                .map(shopConfigRef -> {
                    Reference respRef = new Reference();
                    respRef.setShopConfig(shopConfigRef);
                    return respRef;
                })
                .toList();
    }

    private List<VersionedObject> getVersionedObjects(List<Reference> references) {
        try {
            VersionReference versionRef = new VersionReference();
            versionRef.setHead(new Head());
            return dominantClient.checkoutObjects(versionRef, references);
        } catch (TException e) {
            log.error("Error while get objects for references from dominant: {}", references, e);
            return Collections.emptyList();
        }
    }
}
