package br.com.clyvo.kura.tutor.consentimento.lgpd;

import jakarta.servlet.http.HttpServletRequest;

public record AuditoriaSessao(String ipCliente, String userAgent) {

    public static AuditoriaSessao from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return new AuditoriaSessao(ip, request.getHeader("User-Agent"));
    }
}
