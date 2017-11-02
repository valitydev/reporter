package com.rbkmoney.reporter;

import org.springframework.core.io.ClassPathResource;

/**
 * Created by tolkonepiu on 13/07/2017.
 */
public enum ReportType {
    provision_of_service("templates/provision_of_service_act.xlsx"),
    payment_registry("templates/payment_registry.xlsx");

    ClassPathResource templateResource;

    ReportType(String template) {
        this.templateResource = new ClassPathResource(template);
    }

    public ClassPathResource getTemplateResource() {
        return templateResource;
    }

}
