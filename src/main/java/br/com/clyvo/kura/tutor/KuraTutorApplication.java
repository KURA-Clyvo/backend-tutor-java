package br.com.clyvo.kura.tutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Ponto de entrada do KURA · Backend Tutor.
 *
 * <p>Responsabilidades deste serviço (bounded context "Tutor"):
 * <ul>
 *   <li>Identidade e autenticação do tutor ({@code CONTA_TUTOR})
 *   <li>Consentimentos LGPD ({@code CONSENTIMENTO})
 *   <li>Agendamentos ({@code AGENDAMENTO})
 *   <li>Leitura de tutores, pets, espécies e raças (compartilhado com .NET)
 * </ul>
 *
 * <p>O domínio clínico (eventos, vacinas, prescrições) pertence ao .NET do Felipe.
 * Este serviço consome a API clínica via RestTemplate quando precisa da timeline.
 */
@SpringBootApplication
@EnableCaching          // habilita @Cacheable nos services
@EnableJpaAuditing      // habilita @CreatedDate / @LastModifiedDate nas entidades
public class KuraTutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(KuraTutorApplication.class, args);
    }
}
