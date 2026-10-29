package br.com.clyvo.kura.tutor.shared.mongo;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Leitura única de arquivo versionado em {@code src/main/resources} (schema, índices, seed). */
public final class RecursoClasspath {

    private RecursoClasspath() {
    }

    public static String lerTexto(String caminho) {
        try {
            return new String(new ClassPathResource(caminho).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Arquivo versionado ausente: " + caminho, e);
        }
    }
}
