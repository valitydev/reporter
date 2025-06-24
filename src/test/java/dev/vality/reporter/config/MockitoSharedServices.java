package dev.vality.reporter.config;

import dev.vality.reporter.service.ScheduleReports;
import dev.vality.reporter.service.StorageService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {StorageService.class, ScheduleReports.class})
public @interface MockitoSharedServices {
}
