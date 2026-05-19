package br.com.clyvo.kura.tutor.lgpd;

/**
 * Encapsula dados de sessão coletados no controller para fins de auditoria LGPD.
 *
 * Transporta IP real do cliente (considerando proxies Azure/Nginx) e a versão
 * do termo para garantir rastreabilidade exigida pelo art. 7o, I da LGPD.
 *
 * Usado em: ConsentimentoService.registrar()
 */
public record AuditoriaSessao(
    String ipCliente,
    String userAgent
) {
    /**
     * Extrai IP real considerando X-Forwarded-For de proxies reversos.
     * Em producao Azure, o Front Door adiciona o IP real neste header.
     */
    public static AuditoriaSessao from(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        return new AuditoriaSessao(ip, ua);
    }
}
