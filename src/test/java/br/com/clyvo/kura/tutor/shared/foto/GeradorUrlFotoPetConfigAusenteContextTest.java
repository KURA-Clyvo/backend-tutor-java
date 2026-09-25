package br.com.clyvo.kura.tutor.shared.foto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FT-05 (KURA_BACKLOG_FOTO_PET) — aceite: "config ausente ⇒ null + processo sobe".
 *
 * <p>Sobrescreve os defaults de desenvolvimento de {@code application-dev.yml} (que HABILITAM
 * a assinatura por conveniência local) para simular o cenário real de configuração ausente —
 * exatamente o que aconteceria em produção sem {@code FOTO_URL_SECRET}/{@code FOTO_URL_BASE}.
 * Contexto Spring completo: prova que {@link GeradorUrlFotoPet} não impede o boot (diferente
 * do fail-fast do {@code AssinadorUrlFotoHmac} do .NET para quem ESCREVE — decisão do maestro
 * documentada na classe).
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "kura.foto.url-secret=",
        "kura.foto.url-base="
})
class GeradorUrlFotoPetConfigAusenteContextTest {

    @Autowired
    private GeradorUrlFotoPet geradorUrlFotoPet;

    @Test
    @DisplayName("contexto sobe normalmente mesmo com FOTO_URL_SECRET/FOTO_URL_BASE vazios")
    void contextoSobeComConfigAusente() {
        assertThat(geradorUrlFotoPet).isNotNull();
    }

    @Test
    @DisplayName("gerarUrl devolve null com config ausente, mesmo para chave válida")
    void gerarUrl_devolveNullComConfigAusente() {
        String url = geradorUrlFotoPet.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB);

        assertThat(url).isNull();
    }
}
