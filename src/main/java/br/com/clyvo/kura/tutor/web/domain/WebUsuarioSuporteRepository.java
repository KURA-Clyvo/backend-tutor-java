// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebUsuarioSuporteRepository extends JpaRepository<WebUsuarioSuporte, Long> {

    Optional<WebUsuarioSuporte> findByDsLogin(String dsLogin);
}
