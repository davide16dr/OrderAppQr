package com.orderapp.ordering.config;

import io.sentry.SentryOptions;
import io.sentry.protocol.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

@Slf4j
@Configuration
public class SentryConfiguration {

    /**
     * Arricchisce ogni evento Sentry con il tenantId e userId dell'utente
     * autenticato, estratti dai dettagli del token JWT.
     */
    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
        return (event, hint) -> {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth instanceof UsernamePasswordAuthenticationToken token
                        && token.getDetails() instanceof Map<?, ?> details) {

                    Object tenantId = details.get("tenantId");
                    Object userId   = details.get("userId");

                    if (tenantId != null) event.setTag("tenant_id", String.valueOf(tenantId));
                    if (userId   != null) {
                        User sentryUser = new User();
                        sentryUser.setId(String.valueOf(userId));
                        event.setUser(sentryUser);
                    }
                }
            } catch (Exception ignored) {
                // Non bloccare mai un evento Sentry per errori nel callback
            }
            return event;
        };
    }
}
