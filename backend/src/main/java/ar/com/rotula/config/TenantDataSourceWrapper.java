package ar.com.rotula.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Intercepts the auto-configured HikariCP DataSource bean and wraps it with
 * TenantAwareDataSource, which sets app.current_tenant on every connection borrow.
 * Wrapping via BeanPostProcessor keeps Spring Boot's auto-configuration intact
 * (Flyway, Hibernate, Actuator) while transparently adding the RLS behavior.
 */
@Component
public class TenantDataSourceWrapper implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ("dataSource".equals(beanName) && bean instanceof DataSource ds) {
            return new TenantAwareDataSource(ds);
        }
        return bean;
    }
}
