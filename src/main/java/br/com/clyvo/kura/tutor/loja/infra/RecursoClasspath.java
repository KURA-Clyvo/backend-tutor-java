// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Leitura única de arquivo versionado em {@code src/main/resources} (schema, índices, seed). */
final class RecursoClasspath {

    private RecursoClasspath() {
    }

    static String lerTexto(String caminho) {
        try {
            return new String(new ClassPathResource(caminho).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Arquivo da Loja ausente: " + caminho, e);
        }
    }
}
