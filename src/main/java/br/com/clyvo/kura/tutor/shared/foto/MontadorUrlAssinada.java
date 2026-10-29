package br.com.clyvo.kura.tutor.shared.foto;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

/**
 * Montagem comum das URLs assinadas servidas pelo .NET (fotos de pet e arquivos de laudo):
 * {@code {base}{prefixo}{chave}?exp={exp}&sig={sig}}, com o MESMO {@link AssinadorUrlFotoHmac}
 * e o MESMO segredo. Os dois geradores só decidem prefixo, validade e quais chaves aceitam.
 */
public final class MontadorUrlAssinada {

    public static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    private final AssinadorUrlFotoHmac assinador;
    private final String baseUrl;
    private final Clock clock;

    private MontadorUrlAssinada(AssinadorUrlFotoHmac assinador, String baseUrl, Clock clock) {
        this.assinador = assinador;
        this.baseUrl = baseUrl;
        this.clock = clock;
    }

    /** Devolve {@code null} quando o segredo é ausente/curto (&lt; 32 bytes UTF-8) ou a base é vazia. */
    public static MontadorUrlAssinada criarOuNulo(String segredo, String baseUrl, Clock clock) {
        boolean segredoValido = segredo != null
                && segredo.getBytes(StandardCharsets.UTF_8).length >= TAMANHO_MINIMO_SEGREDO_BYTES;
        boolean baseValida = baseUrl != null && !baseUrl.isBlank();
        if (!segredoValido || !baseValida) {
            return null;
        }
        return new MontadorUrlAssinada(new AssinadorUrlFotoHmac(segredo), removerBarraFinal(baseUrl), clock);
    }

    public String montar(String prefixo, String chave, Duration validade) {
        long exp = clock.instant().plusSeconds(validade.toSeconds()).getEpochSecond();
        String sig = assinador.assinar(chave, exp);
        return baseUrl + prefixo + chave + "?exp=" + exp + "&sig=" + sig;
    }

    private static String removerBarraFinal(String url) {
        int fim = url.length();
        while (fim > 0 && url.charAt(fim - 1) == '/') {
            fim--;
        }
        return url.substring(0, fim);
    }
}
