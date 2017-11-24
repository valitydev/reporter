package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.domain.CategoryRef;
import com.rbkmoney.damsel.domain.CategoryType;
import com.rbkmoney.damsel.domain.DomainObject;
import com.rbkmoney.damsel.domain_config.Head;
import com.rbkmoney.damsel.domain_config.Reference;
import com.rbkmoney.damsel.domain_config.RepositorySrv;
import com.rbkmoney.damsel.domain_config.Snapshot;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DomainConfigService {

    private final RepositorySrv.Iface domainConfigClient;

    public DomainConfigService(RepositorySrv.Iface domainConfigClient) {
        this.domainConfigClient = domainConfigClient;
    }

    public CategoryType getCategoryType(int categoryId) {
        try {
            Snapshot snapshot = domainConfigClient.checkout(Reference.head(new Head()));
            DomainObject domainObject = snapshot.getDomain().get(new CategoryRef(categoryId));
            return domainObject.getCategory().getData().getType();
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get category type, categoryId='%d'", categoryId), ex);
        }
    }

    public Map<Integer, CategoryType> getCategories() {
        try {
            Snapshot snapshot = domainConfigClient.checkout(Reference.head(new Head()));
            return snapshot.getDomain().entrySet().stream()
                    .filter(entry -> entry.getKey().isSetCategory())
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().getCategory().getId(),
                            entry -> entry.getValue().getCategory().getData().getType()
                            )
                    );
        } catch (TException ex) {
            throw new RuntimeException("Failed to get categories from domain", ex);
        }
    }

    public Map<Integer, CategoryType> getTestCategories() {
        return getCategories().entrySet().stream()
                .filter(entry -> entry.getValue() == CategoryType.test)
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

}
