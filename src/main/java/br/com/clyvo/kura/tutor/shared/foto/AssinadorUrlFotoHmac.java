package br.com.clyvo.kura.tutor.shared.foto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Assinador HMAC-SHA256 de URL de foto de pet — FT-05 (KURA_BACKLOG_FOTO_PET), espelhando
 * byte a byte o algoritmo do {@code backend-clinica-dotnet} (regra 11 do CLAUDE.md — âncora
 * obrigatória para cópia de regra de negócio que mora em outro repo).
 *
 * <p><b>Âncora:</b> {@code backend-clinica-dotnet} {@code main} @ {@code 5adb9e5}, conferida
 * pelo maestro em 2026-09-25 — {@code src/Kura.Infrastructure/Storage/AssinadorUrlFotoHmac.cs}
 * método {@code Assinar} (linhas 40-47) e {@code CalcularMac} (linhas 75-82). Reproduzir com:
 * {@code git -C backend-clinica-dotnet show 5adb9e5:src/Kura.Infrastructure/Storage/AssinadorUrlFotoHmac.cs}
 *
 * <p><b>Fórmula (idêntica ao .NET):</b> mensagem = {@code chave + "\n" + exp} (LF literal —
 * em Java um literal {@code "\n"} num arquivo-fonte compila sempre para o byte 0x0A,
 * independente de plataforma, então não existe aqui o equivalente ao risco
 * {@code Environment.NewLine} do C# em Windows); {@code exp} = segundos Unix, inteiro,
 * decimal (sem separador de milhar — {@code Long.toString} já é invariante de locale, ao
 * contrário de formatação de número real); {@code HMAC-SHA256(key = UTF-8(segredo), msg =
 * UTF-8(mensagem))}; saída em base64url SEM padding.
 *
 * <p><b>Base64url sem padding:</b> {@link Base64#getUrlEncoder()} do JDK já produz o alfabeto
 * RFC 4648 §5 ({@code -}/{@code _} no lugar de {@code +}/{@code /}), e
 * {@code .withoutPadding()} omite o {@code =} — resultado byte a byte idêntico ao do .NET, que
 * monta isso manualmente ({@code Convert.ToBase64String} + {@code TrimEnd('=')} +
 * {@code Replace}). Não reimplementado manualmente aqui de propósito: usar o encoder padrão do
 * JDK para uma transformação padronizada (RFC) reduz risco de bug de reimplementação; o vetor
 * fixo do teste é quem prova a equivalência de ponta a ponta com o .NET, não a leitura do
 * código.
 *
 * <p><b>Este lado só GERA (assina) URLs — nunca valida.</b> Quem valida a assinatura é o
 * {@code backend-clinica-dotnet} (endpoint {@code GET /api/v1/fotos/{*chave}}), que já foi
 * revisado (G2 {@code g2-ft01-ft02.md}, achado F3-a) por aceitar múltiplas grafias da mesma
 * assinatura no {@code Validar} (base64 com padding, alfabeto misto {@code +}/{@code -},
 * {@code /}/{@code _}, e os 2 bits sem uso do 43º caractere). Como este lado não implementa
 * {@code Validar}, o achado F3-a ("comparar bytes, não strings") não se aplica a esta classe —
 * ele só importaria se este lado também comparasse uma {@code sig} recebida contra uma calculada,
 * o que nunca acontece aqui.
 */
public final class AssinadorUrlFotoHmac {

    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] segredoBytes;

    public AssinadorUrlFotoHmac(String segredo) {
        if (segredo == null || segredo.isEmpty()) {
            throw new IllegalArgumentException("Segredo de assinatura de URL de foto não pode ser vazio.");
        }
        this.segredoBytes = segredo.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Assina {@code chave} com validade até {@code expEpochSeconds} (segundos Unix). Devolve a
     * assinatura em base64url sem padding — mesma saída que o .NET produziria para o mesmo
     * trio (segredo, chave, exp).
     */
    public String assinar(String chave, long expEpochSeconds) {
        if (chave == null || chave.isEmpty()) {
            throw new IllegalArgumentException("Chave a assinar não pode ser vazia.");
        }
        byte[] mac = calcularMac(chave, expEpochSeconds);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac);
    }

    private byte[] calcularMac(String chave, long expEpochSeconds) {
        String mensagem = chave + "\n" + expEpochSeconds;
        byte[] mensagemBytes = mensagem.getBytes(StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredoBytes, ALGORITMO));
            return mac.doFinal(mensagemBytes);
        } catch (GeneralSecurityException e) {
            // HmacSHA256 é algoritmo padrão do JDK (JCA) — nunca deveria faltar; se faltar, é
            // erro de ambiente, não entrada do chamador, então lança unchecked (mesma postura
            // do .NET, que não trata essa classe de falha como "false").
            throw new IllegalStateException("Falha ao calcular HMAC-SHA256 da URL de foto.", e);
        }
    }
}
