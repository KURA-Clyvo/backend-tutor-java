package br.com.clyvo.kura.tutor.shared.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Configures pt-BR as the default locale and wires Bean Validation to use
 * the Spring MessageSource, so annotation messages can be externalized via
 * the {key} syntax in messages_pt_BR.properties.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    private final MessageSource messageSource;

    public LocaleConfig(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("pt-BR"));
        return resolver;
    }

    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource);
        return factory;
    }
}
