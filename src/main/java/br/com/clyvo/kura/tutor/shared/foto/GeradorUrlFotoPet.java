package br.com.clyvo.kura.tutor.shared.foto;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gera as URLs assinadas de foto de pet expostas ao tutor — FT-05 (KURA_BACKLOG_FOTO_PET).
 *
 * <p><b>Mesmo algoritmo do .NET</b> (ver {@link AssinadorUrlFotoHmac} e {@link ChaveFotoPet}
 * para as âncoras). Formato da URL, espelhando {@code GeradorUrlFotoPet.cs} do
 * {@code backend-clinica-dotnet} ({@code main} @ {@code 5adb9e5}, linhas 47-61):
 * {@code {base}/api/v1/fotos/{chaveVariante}?exp={exp}&sig={sig}}.
 *
 * <p><b>Diferença deliberada em relação ao .NET (decisão do maestro, brief FT-05):</b> o .NET
 * deriva a base da URL do {@code HttpContext} quando {@code Foto:UrlBase} não está configurado
 * (ele É o host que serve a foto). Este lado NUNCA deriva da requisição — o Java não é o host
 * do arquivo, só assina a URL que aponta para o .NET (achado F-P1 do G2 {@code g2-ft04.md}: a
 * base certa é a origem HTTPS que os apps alcançam, a MESMA configurada para o .NET via
 * {@code FOTO_URL_BASE} — FT-06). Por isso aqui a base é SEMPRE explícita
 * ({@code kura.foto.url-base}), nunca derivada de request.
 *
 * <p><b>Segredo/base ausentes ou segredo curto (&lt; 32 bytes UTF-8) NÃO derruba o processo</b>
 * (decisão do maestro, diferente do .NET, que faz fail-fast em {@code AssinadorUrlFotoHmac}
 * para o lado que ESCREVE): as URLs saem {@code null} — o app mostra a ilustração padrão — e um
 * único {@code WARN} é logado na partida (nunca o valor do segredo). Justificativa: no NEXT, o
 * segredo é compartilhado por 2 serviços (.NET e Java) via a MESMA variável de ambiente
 * ({@code FOTO_URL_SECRET}, FT-06); se um dos dois subir sem ela configurada num ambiente de
 * demonstração, o produto tem que continuar de pé (ilustração no lugar da foto), não cair.
 */
@Component
public class GeradorUrlFotoPet {

    private static final Logger log = LoggerFactory.getLogger(GeradorUrlFotoPet.class);
    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;
    private static final double VALIDADE_HORAS_PADRAO = 24;
    private static final String PREFIXO_ENDPOINT = "/api/v1/fotos/";

    private final String baseUrl;
    private final double validadeHoras;
    private final Clock clock;
    private final AssinadorUrlFotoHmac assinador;

    public GeradorUrlFotoPet(
            @Value("${kura.foto.url-secret:}") String segredo,
            @Value("${kura.foto.url-base:}") String baseUrlConfigurada,
            @Value("${kura.foto.url-validade-horas:" + VALIDADE_HORAS_PADRAO + "}") double validadeHoras,
            Clock clock) {
        this.clock = clock;
        this.validadeHoras = validadeHoras;

        boolean segredoValido = segredo != null
                && segredo.getBytes(StandardCharsets.UTF_8).length >= TAMANHO_MINIMO_SEGREDO_BYTES;
        boolean baseValida = baseUrlConfigurada != null && !baseUrlConfigurada.isBlank();

        if (segredoValido && baseValida) {
            this.assinador = new AssinadorUrlFotoHmac(segredo);
            this.baseUrl = removerBarraFinal(baseUrlConfigurada);
        } else {
            this.assinador = null;
            this.baseUrl = null;
            log.warn(
                    "URLs assinadas de foto de pet DESABILITADAS: kura.foto.url-secret "
                            + "ausente/curto (<{} bytes UTF-8) ou kura.foto.url-base ausente. "
                            + "dsFotoUrl/dsFotoThumbUrl sairão null nos DTOs do tutor — o app usa a "
                            + "ilustração padrão. Configurar FOTO_URL_SECRET/FOTO_URL_BASE (FT-06) "
                            + "para habilitar.",
                    TAMANHO_MINIMO_SEGREDO_BYTES);
        }
    }

    /**
     * Gera a URL assinada da variante {@code sufixoTamanho} da foto de {@code chaveBase}.
     * Devolve {@code null} quando não há chave (pet sem foto) OU quando a assinatura está
     * desabilitada (config ausente/inválida) — os dois casos têm o mesmo efeito no app: mostrar
     * a ilustração padrão em vez de uma foto.
     */
    public String gerarUrl(String chaveBase, String sufixoTamanho) {
        if (chaveBase == null || chaveBase.isBlank() || assinador == null) {
            return null;
        }

        String chaveVariante = ChaveFotoPet.variante(chaveBase, sufixoTamanho);

        long validadeSegundos = Math.round(validadeHoras * 3600.0);
        Instant expiraEm = clock.instant().plusSeconds(validadeSegundos);
        long exp = expiraEm.getEpochSecond();

        String sig = assinador.assinar(chaveVariante, exp);

        return baseUrl + PREFIXO_ENDPOINT + chaveVariante + "?exp=" + exp + "&sig=" + sig;
    }

    private static String removerBarraFinal(String url) {
        int fim = url.length();
        while (fim > 0 && url.charAt(fim - 1) == '/') {
            fim--;
        }
        return url.substring(0, fim);
    }
}
