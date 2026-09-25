package br.com.clyvo.kura.tutor.shared.foto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FT-05 — espelha {@code ChaveFotoPetTests} do backend-clinica-dotnet (mesma fórmula, ver
 * âncora em {@link ChaveFotoPet}).
 */
class ChaveFotoPetTest {

    @Test
    @DisplayName("variante — insere o sufixo ANTES da extensão")
    void variante_insereSufixoAntesDaExtensao() {
        assertThat(ChaveFotoPet.variante("clinica/7/pet/12/abc.webp", ChaveFotoPet.SUFIXO_THUMB))
                .isEqualTo("clinica/7/pet/12/abc_256.webp");
        assertThat(ChaveFotoPet.variante("clinica/7/pet/12/abc.webp", ChaveFotoPet.SUFIXO_MEDIA))
                .isEqualTo("clinica/7/pet/12/abc_1080.webp");
    }

    @Test
    @DisplayName("variante — funciona para as 3 extensões aceitas (webp/jpg/png)")
    void variante_funcionaParaAs3Extensoes() {
        assertThat(ChaveFotoPet.variante("clinica/1/pet/2/x.jpg", "256"))
                .isEqualTo("clinica/1/pet/2/x_256.jpg");
        assertThat(ChaveFotoPet.variante("clinica/1/pet/2/x.png", "1080"))
                .isEqualTo("clinica/1/pet/2/x_1080.png");
    }

    @Test
    @DisplayName("variante — chave sem extensão (sem ponto) LANÇA, não devolve fallback silencioso")
    void variante_semExtensao_lanca() {
        assertThatThrownBy(() -> ChaveFotoPet.variante("clinica/7/pet/12/abc", "256"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não tem extensão");
    }
}
