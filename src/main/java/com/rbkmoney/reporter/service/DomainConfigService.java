package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.domain.CategoryRef;
import com.rbkmoney.damsel.domain.CategoryType;
import com.rbkmoney.damsel.domain.DomainObject;
import com.rbkmoney.damsel.domain_config.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DomainConfigService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private RepositoryClientSrv.Iface dominantClient;

    @Autowired
    public DomainConfigService(RepositoryClientSrv.Iface dominantClient) {
        this.dominantClient = dominantClient;
    }

    public CategoryType getCategoryType(CategoryRef categoryRef) {
        return getCategoryType(categoryRef, Reference.head(new Head()));
    }

    public CategoryType getCategoryType(CategoryRef categoryRef, long domainRevision) {
        return getCategoryType(categoryRef, Reference.version(domainRevision));
    }

    public CategoryType getCategoryType(CategoryRef categoryRef, Reference revisionReference) {
        log.info("Trying to get category type, categoryRef='{}', revisionReference='{}'", categoryRef, revisionReference);
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setCategory(categoryRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(revisionReference, reference);
            CategoryType categoryType = versionedObject.getObject().getCategory().getData().getType();
            log.info("Category type has been found, categoryRef='{}', revisionReference='{}', categoryType='{}'", categoryRef, revisionReference, categoryType);
            return categoryType;
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get category type, categoryRef='%s', revisionReference='%s'", categoryRef, revisionReference), ex);
        }
    }

}
