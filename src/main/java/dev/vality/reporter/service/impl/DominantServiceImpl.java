package dev.vality.reporter.service.impl;

import dev.vality.damsel.domain.Currency;
import dev.vality.damsel.domain.CurrencyRef;
import dev.vality.damsel.domain_config.*;
import dev.vality.reporter.exception.DominantException;
import dev.vality.reporter.exception.NotFoundException;
import dev.vality.reporter.service.DominantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DominantServiceImpl implements DominantService {

    private final RepositoryClientSrv.Iface dominantClient;

    @Cacheable(value = "currencies", cacheManager = "currenciesCacheManager")
    public Currency getCurrency(String symbolicCode) {
        CurrencyRef ref = new CurrencyRef();
        ref.setSymbolicCode(symbolicCode);
        return getCurrency(ref, Reference.head(new Head()));
    }

    private Currency getCurrency(CurrencyRef currencyRef, Reference revisionReference)
            throws NotFoundException {
        log.debug("Trying to get currency, currencyRef='{}', revisionReference='{}'", currencyRef, revisionReference);
        try {
            var reference = new dev.vality.damsel.domain.Reference();
            reference.setCurrency(currencyRef);
            VersionedObject versionedObject = checkoutObject(revisionReference, reference);
            Currency currency = versionedObject.getObject().getCurrency().getData();
            log.debug("Currency has been found, currencyRef='{}', revisionReference='{}', currency='{}'",
                    currencyRef, revisionReference, currency);
            return currency;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, currencyRef='%s', revisionReference='%s'",
                    currencyRef, revisionReference), ex);
        } catch (TException ex) {
            throw new DominantException(String.format("Failed to get currency, currencyRef='%s', " +
                    "revisionReference='%s'", currencyRef, revisionReference), ex);
        }
    }

    private VersionedObject checkoutObject(Reference revisionReference, dev.vality.damsel.domain.Reference reference)
            throws TException {
        return dominantClient.checkoutObject(revisionReference, reference);
    }
}
