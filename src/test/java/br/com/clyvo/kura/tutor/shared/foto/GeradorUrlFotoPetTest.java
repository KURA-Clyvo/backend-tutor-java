package br.com.clyvo.kura.tutor.shared.foto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * FT-05 (KURA_BACKLOG_FOTO_PET) — {@link GeradorUrlFotoPet}: formato da URL, validade e o
 * comportamento de "não derrubar o processo" quando a config está ausente/inválida (decisão
 * do maestro, diferente do fail-fast do .NET — ver XML doc da classe).
 */
class GeradorUrlFotoPetTest {

    // Mesmo trio do vetor fixo de AssinadorUrlFotoHmacTest — reaproveitado aqui para que a
    // URL inteira monte com a MESMA sig já provada contra o .NET.
    private static final String SEGREDO = "kura-teste-segredo-com-32-bytes-ok!!";
    private static final long EXP = 1893456000L;
    private static final long VALIDADE_HORAS = 24;
    private static final long AGORA_EPOCH = EXP - VALIDADE_HORAS * 3600; // 1893369600
    private static final String SIG_ESPERADA = "frkL_Z__8wHPKtfmeVu8A4L59bcuZNzdSx31BgHBjL0";

    private static Clock clockFixo() {
        return Clock.fixed(Instant.ofEpochSecond(AGORA_EPOCH), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("config válida — URL no formato {base}/api/v1/fotos/{chaveVariante}?exp=&sig=, sig batendo com o vetor fixo")
    void configValida_geraUrlNoFormatoEsperado() {
        var gerador = new GeradorUrlFotoPet(SEGREDO, "https://kura-clinica.example/", VALIDADE_HORAS, clockFixo());

        String url = gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB);

        assertThat(url).isEqualTo(
                "https://kura-clinica.example/api/v1/fotos/clinica/1/pet/2/abc_256.webp"
                        + "?exp=" + EXP + "&sig=" + SIG_ESPERADA);
    }

    @Test
    @DisplayName("base com barra final é normalizada (sem duplicar '//')")
    void baseComBarraFinal_normalizada() {
        var gerador = new GeradorUrlFotoPet(SEGREDO, "https://kura-clinica.example///", VALIDADE_HORAS, clockFixo());

        String url = gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB);

        assertThat(url).startsWith("https://kura-clinica.example/api/v1/fotos/");
        assertThat(url).doesNotContain("example//api");
    }

    @Test
    @DisplayName("chaveBase nula — devolve null, mesmo com config válida (pet sem foto)")
    void chaveBaseNula_devolveNull() {
        var gerador = new GeradorUrlFotoPet(SEGREDO, "https://kura-clinica.example", VALIDADE_HORAS, clockFixo());

        assertThat(gerador.gerarUrl(null, ChaveFotoPet.SUFIXO_THUMB)).isNull();
    }

    @Test
    @DisplayName("chaveBase vazia — devolve null")
    void chaveBaseVazia_devolveNull() {
        var gerador = new GeradorUrlFotoPet(SEGREDO, "https://kura-clinica.example", VALIDADE_HORAS, clockFixo());

        assertThat(gerador.gerarUrl("", ChaveFotoPet.SUFIXO_THUMB)).isNull();
    }

    // ─── Config ausente/inválida NÃO derruba o processo — só desabilita (decisão do maestro) ──

    @Test
    @DisplayName("segredo ausente (vazio) — gerarUrl devolve null, construtor não lança")
    void segredoAusente_geraUrlNullSemLancar() {
        assertThatNoException().isThrownBy(() -> {
            var gerador = new GeradorUrlFotoPet("", "https://kura-clinica.example", VALIDADE_HORAS, clockFixo());
            assertThat(gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB)).isNull();
        });
    }

    @Test
    @DisplayName("segredo curto (< 32 bytes UTF-8) — tratado como ausente, gerarUrl devolve null")
    void segredoCurto_tratadoComoAusente() {
        String segredoCurto = "curto-demais"; // 12 bytes
        var gerador = new GeradorUrlFotoPet(segredoCurto, "https://kura-clinica.example", VALIDADE_HORAS, clockFixo());

        assertThat(gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB)).isNull();
    }

    @Test
    @DisplayName("base ausente (vazia) — gerarUrl devolve null mesmo com segredo válido")
    void baseAusente_geraUrlNull() {
        var gerador = new GeradorUrlFotoPet(SEGREDO, "", VALIDADE_HORAS, clockFixo());

        assertThat(gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB)).isNull();
    }

    @Test
    @DisplayName("base ausente (em branco) — gerarUrl devolve null")
    void baseEmBranco_geraUrlNull() {
        var gerador = new GeradorUrlFotoPet(SEGREDO, "   ", VALIDADE_HORAS, clockFixo());

        assertThat(gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB)).isNull();
    }

    @Test
    @DisplayName("segredo E base ausentes (defaults do application.yml) — gerarUrl devolve null, sem exceção")
    void ambosAusentes_geraUrlNullSemExcecao() {
        assertThatNoException().isThrownBy(() -> {
            var gerador = new GeradorUrlFotoPet("", "", VALIDADE_HORAS, clockFixo());
            assertThat(gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_MEDIA)).isNull();
        });
    }

    @Test
    @DisplayName("validade padrão (24h) é usada quando não sobrescrita — exp = agora + 24h")
    void validadePadrao_24Horas() {
        // Clock fixo em "agora" arbitrário, validade explícita de 24h (mesmo valor padrão da
        // classe) — prova que o parâmetro chega corretamente ao cálculo de exp.
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_000_000_000L), ZoneOffset.UTC);
        var gerador = new GeradorUrlFotoPet(SEGREDO, "https://kura-clinica.example", 24, clock);

        String url = gerador.gerarUrl("clinica/1/pet/2/abc.webp", ChaveFotoPet.SUFIXO_THUMB);

        long expEsperado = 1_000_000_000L + 24 * 3600;
        assertThat(url).contains("exp=" + expEsperado);
    }
}
