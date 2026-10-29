package br.com.clyvo.kura.tutor.laudo.infra;

import br.com.clyvo.kura.tutor.shared.foto.AssinadorUrlFotoHmac;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorUrlLaudoTest {

    // Mesmo segredo/exp do vetor de AssinadorUrlFotoHmacTest (idêntico ao .NET).
    private static final String SEGREDO = "kura-teste-segredo-com-32-bytes-ok!!";
    private static final long EXP = 1893456000L;
    private static final String CHAVE_FOTO_DO_VETOR = "clinica/1/pet/2/abc_256.webp";
    private static final String SIG_VETOR_FOTO = "frkL_Z__8wHPKtfmeVu8A4L59bcuZNzdSx31BgHBjL0";

    private static final String CHAVE = "clinica/1/pet/2/laudo/0a1b2c3d4e5f40008000000000abcdef.pdf";
    // Calculada de forma independente (python hmac/hashlib) para segredo+CHAVE+EXP: mesma função do vetor da foto.
    private static final String SIG_LAUDO = "PwmI8Icj76GX8-yU84jnDKK1P4CcbgwY_-FNkbLZ6Ag";
    private static final long VALIDADE_MIN = 60;

    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(EXP - VALIDADE_MIN * 60), ZoneOffset.UTC);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(GeradorUrlLaudo.class);

    @BeforeEach
    void capturar() {
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(logs);
    }

    @Test
    @DisplayName("configurado: URL {base}/api/v1/laudos/arquivos/{chave}?exp=&sig= com a assinatura esperada (Clock injetado)")
    void configurado() {
        var gerador = new GeradorUrlLaudo(SEGREDO, "https://kura-clinica.example/", VALIDADE_MIN, clock);

        String url = gerador.gerarUrl(2L, CHAVE);

        assertThat(url).isEqualTo("https://kura-clinica.example/api/v1/laudos/arquivos/" + CHAVE
                + "?exp=" + EXP + "&sig=" + SIG_LAUDO);
        assertThat(logs.list).isEmpty();
    }

    @Test
    @DisplayName("é a MESMA função de assinatura das fotos: o vetor da foto reproduz com o mesmo segredo")
    void mesmaFuncaoDaFoto() {
        assertThat(new AssinadorUrlFotoHmac(SEGREDO).assinar(CHAVE_FOTO_DO_VETOR, EXP)).isEqualTo(SIG_VETOR_FOTO);
        assertThat(new AssinadorUrlFotoHmac(SEGREDO).assinar(CHAVE, EXP)).isEqualTo(SIG_LAUDO);
    }

    @Test
    @DisplayName("sem base ou segredo curto: urlArquivo null, 1 WARN na partida e nunca o valor do segredo")
    void semConfiguracao() {
        var semBase = new GeradorUrlLaudo(SEGREDO, "", VALIDADE_MIN, clock);
        var segredoCurto = new GeradorUrlLaudo("curto", "https://x.example", VALIDADE_MIN, clock);

        assertThat(semBase.gerarUrl(2L, CHAVE)).isNull();
        assertThat(segredoCurto.gerarUrl(2L, CHAVE)).isNull();
        assertThat(logs.list).hasSize(2).allSatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).doesNotContain(SEGREDO).doesNotContain("x.example");
        });
    }

    @Test
    @DisplayName("chave de OUTRO pet (ou malformada): null + WARN sem a chave inteira")
    void chaveDeOutroPet() {
        var gerador = new GeradorUrlLaudo(SEGREDO, "https://kura-clinica.example", VALIDADE_MIN, clock);

        assertThat(gerador.gerarUrl(3L, CHAVE)).isNull();
        assertThat(gerador.gerarUrl(2L, "clinica/1/pet/2/laudo/../../x.pdf")).isNull();
        assertThat(gerador.gerarUrl(2L, "clinica/1/pet/2/laudo/0a1b.exe")).isNull();
        assertThat(gerador.gerarUrl(2L, null)).isNull();
        assertThat(logs.list).hasSize(3).allSatisfy(e ->
                assertThat(e.getFormattedMessage()).doesNotContain("0a1b2c3d").doesNotContain("clinica/"));
    }
}
