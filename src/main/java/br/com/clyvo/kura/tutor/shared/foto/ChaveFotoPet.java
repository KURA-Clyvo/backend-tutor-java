package br.com.clyvo.kura.tutor.shared.foto;

/**
 * Fórmula de chave de foto de pet — lado LEITURA (backend-tutor-java, FT-05). Espelha
 * {@code ChaveFotoPet.Variante} do {@code backend-clinica-dotnet} (regra 11 do CLAUDE.md —
 * cópia de regra de negócio de outro repo exige âncora).
 *
 * <p><b>Âncora:</b> {@code backend-clinica-dotnet} {@code main} @ {@code 5adb9e5}, conferida
 * pelo maestro em 2026-09-25 — {@code src/Kura.Domain/Storage/ChaveFotoPet.cs}, método
 * {@code Variante} (linhas 56-69). Reproduzir com:
 * {@code git -C backend-clinica-dotnet show 5adb9e5:src/Kura.Domain/Storage/ChaveFotoPet.cs}
 *
 * <p>Este lado só lê {@code Pet.dsFotoChave} (a chave BASE, já gravada pelo .NET via FT-03) e
 * deriva as chaves de VARIANTE para montar as 2 URLs do DTO — nunca monta a chave base (isso é
 * exclusivo do .NET, que faz o upload). Por isso só {@code variante(...)} existe aqui; não há
 * equivalente Java a {@code ChaveFotoPet.Base(...)}.
 */
public final class ChaveFotoPet {

    /** Sufixo de tamanho da variante de lista/avatar (256px) — regra A5 do backlog. */
    public static final String SUFIXO_THUMB = "256";

    /** Sufixo de tamanho da variante de detalhe (1080px) — regra A5 do backlog. */
    public static final String SUFIXO_MEDIA = "1080";

    private ChaveFotoPet() {}

    /**
     * Deriva a chave de uma variante a partir da chave base, inserindo
     * {@code sufixoTamanho} ANTES da extensão. Ex.:
     * {@code variante("clinica/7/pet/12/abc.webp", "256")} → {@code "clinica/7/pet/12/abc_256.webp"}.
     *
     * @throws IllegalArgumentException se {@code chaveBase} não tiver extensão (sem ponto) —
     *     mesma postura do .NET (fix wave G2 {@code g2-ft01-ft02.md}, achado G2-f): uma chave
     *     sem extensão que sobrevivesse em silêncio seria pior do que falhar cedo, porque toda
     *     chave base gravada pelo .NET SEMPRE tem extensão.
     */
    public static String variante(String chaveBase, String sufixoTamanho) {
        int indicePonto = chaveBase.lastIndexOf('.');
        if (indicePonto < 0) {
            throw new IllegalArgumentException(
                    "Chave base '" + chaveBase + "' não tem extensão — só chaves gravadas pelo "
                            + "backend-clinica-dotnet (ChaveFotoPet.Base()) são aceitas.");
        }
        String semExtensao = chaveBase.substring(0, indicePonto);
        String extensao = chaveBase.substring(indicePonto + 1);
        return semExtensao + "_" + sufixoTamanho + "." + extensao;
    }
}
