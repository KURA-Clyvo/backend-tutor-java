package br.com.clyvo.kura.tutor.laudo.infra;

import br.com.clyvo.kura.tutor.shared.foto.MontadorUrlAssinada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL assinada do arquivo de um laudo, servida pela rota da .NET
 * {@code GET /api/v1/laudos/arquivos/{chave}?exp=&sig=}. Mesmo HMAC e mesmo segredo
 * ({@code kura.foto.url-secret}) das fotos; a separação vem da rota e da regex fechada da .NET.
 * Sem base/segredo válidos, ou com chave malformada / de outro pet, devolve {@code null}
 * (a tela mostra só os metadados) e nunca derruba nada.
 */
@Component
public class GeradorUrlLaudo {

    private static final Logger log = LoggerFactory.getLogger(GeradorUrlLaudo.class);
    private static final String PREFIXO_ENDPOINT = "/api/v1/laudos/arquivos/";
    private static final Pattern CHAVE = Pattern.compile(
            "^clinica/\\d+/pet/(\\d+)/laudo/[0-9a-f-]+\\.(pdf|jpg|png|webp)$");

    private final MontadorUrlAssinada montador;
    private final Duration validade;

    public GeradorUrlLaudo(
            @Value("${kura.foto.url-secret:}") String segredo,
            @Value("${kura.laudo.url-base:}") String base,
            @Value("${kura.laudo.url-validade-minutos:60}") long validadeMinutos,
            Clock clock) {
        this.validade = Duration.ofMinutes(validadeMinutos);
        this.montador = MontadorUrlAssinada.criarOuNulo(segredo, base, clock);
        if (montador == null) {
            log.warn("URLs assinadas de laudo DESABILITADAS: kura.foto.url-secret ausente/curto (<{} bytes UTF-8) "
                            + "ou kura.laudo.url-base ausente. urlArquivo sairá null; a lista mostra só os metadados. "
                            + "Configurar FOTO_URL_SECRET/LAUDO_URL_BASE para habilitar.",
                    MontadorUrlAssinada.TAMANHO_MINIMO_SEGREDO_BYTES);
        }
    }

    public String gerarUrl(long idPet, String chave) {
        if (montador == null || chave == null) {
            return null;
        }
        Matcher m = CHAVE.matcher(chave);
        if (!m.matches() || !m.group(1).equals(Long.toString(idPet))) {
            log.warn("Laudo com chave de arquivo malformada ou de outro pet (idPet={}): URL não gerada.", idPet);
            return null;
        }
        return montador.montar(PREFIXO_ENDPOINT, chave, validade);
    }
}
