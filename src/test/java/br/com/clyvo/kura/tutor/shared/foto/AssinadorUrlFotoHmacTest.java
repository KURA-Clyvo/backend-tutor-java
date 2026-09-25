package br.com.clyvo.kura.tutor.shared.foto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FT-05 (KURA_BACKLOG_FOTO_PET) — prova do vetor fixo do brief, IDÊNTICO ao teste do
 * backend-clinica-dotnet: os 2 lados provam a MESMA string a partir do MESMO trio
 * (segredo, chave, exp). Ver a âncora completa em {@link AssinadorUrlFotoHmac}.
 */
class AssinadorUrlFotoHmacTest {

    // Vetor fixo do brief FT-05 — idêntico ao teste .NET (AssinadorUrlFotoHmacTests, FT-02).
    private static final String SEGREDO = "kura-teste-segredo-com-32-bytes-ok!!";
    private static final String CHAVE = "clinica/1/pet/2/abc_256.webp";
    private static final long EXP = 1893456000L;
    private static final String SIG_ESPERADA = "frkL_Z__8wHPKtfmeVu8A4L59bcuZNzdSx31BgHBjL0";

    @Test
    @DisplayName("vetor fixo — assinatura idêntica ao .NET (segredo, chave, exp iguais)")
    void vetorFixo_devolveAssinaturaEsperada() {
        var assinador = new AssinadorUrlFotoHmac(SEGREDO);

        String sig = assinador.assinar(CHAVE, EXP);

        assertThat(sig).isEqualTo(SIG_ESPERADA);
    }

    @Test
    @DisplayName("assinatura é determinística — mesmo trio produz sempre a mesma sig")
    void mesmoTrio_produzSempreAMesmaAssinatura() {
        var assinador = new AssinadorUrlFotoHmac(SEGREDO);

        assertThat(assinador.assinar(CHAVE, EXP)).isEqualTo(assinador.assinar(CHAVE, EXP));
    }

    @Test
    @DisplayName("chave diferente produz assinatura diferente")
    void chaveDiferente_produzAssinaturaDiferente() {
        var assinador = new AssinadorUrlFotoHmac(SEGREDO);

        String sigOutraChave = assinador.assinar("clinica/1/pet/2/abc_1080.webp", EXP);

        assertThat(sigOutraChave).isNotEqualTo(SIG_ESPERADA);
    }

    @Test
    @DisplayName("exp diferente produz assinatura diferente")
    void expDiferente_produzAssinaturaDiferente() {
        var assinador = new AssinadorUrlFotoHmac(SEGREDO);

        String sigOutroExp = assinador.assinar(CHAVE, EXP + 1);

        assertThat(sigOutroExp).isNotEqualTo(SIG_ESPERADA);
    }

    @Test
    @DisplayName("saída não tem padding '=' e usa alfabeto base64url ('-'/'_' em vez de '+'/'/')")
    void saida_ehBase64UrlSemPadding() {
        var assinador = new AssinadorUrlFotoHmac(SEGREDO);

        String sig = assinador.assinar(CHAVE, EXP);

        assertThat(sig).doesNotContain("=", "+", "/");
    }

    @Test
    @DisplayName("chave vazia lança IllegalArgumentException")
    void chaveVazia_lanca() {
        var assinador = new AssinadorUrlFotoHmac(SEGREDO);

        assertThatThrownBy(() -> assinador.assinar("", EXP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("segredo vazio no construtor lança IllegalArgumentException")
    void segredoVazio_lanca() {
        assertThatThrownBy(() -> new AssinadorUrlFotoHmac(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
