package br.com.clyvo.kura.tutor.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configura o CacheManager Caffeine para os catálogos de referência (Espécie e Raça).
 *
 * Política: TTL de 6 horas, máximo 200 entradas por cache.
 * NUNCA cachear: Tutor, Pet, Agendamento, Consentimento.
 *
 * @EnableCaching já está em KuraTutorApplication — não duplicar aqui.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("especies", "racas");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(6, TimeUnit.HOURS));
        return manager;
    }
}
