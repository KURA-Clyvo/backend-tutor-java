package br.com.clyvo.kura.tutor.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável (FT-05, KURA_BACKLOG_FOTO_PET) — mesmo padrão do {@code TimeProvider}
 * singleton do {@code backend-clinica-dotnet} ({@code ServiceCollectionExtensions.cs}):
 * permite a um teste fixar "agora" (ex.: {@link java.time.Clock#fixed}) para gerar/validar uma
 * URL de foto já vencida sem depender de {@code Thread.sleep}. Produção usa
 * {@link Clock#systemUTC()} — {@code exp} da URL é sempre segundos Unix (UTC por definição).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
