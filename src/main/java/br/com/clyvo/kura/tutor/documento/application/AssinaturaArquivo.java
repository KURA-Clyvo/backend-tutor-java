package br.com.clyvo.kura.tutor.documento.application;

import java.util.Optional;

/**
 * Detecta o tipo pelos primeiros bytes (magic bytes); nunca confia em extensão nem em
 * Content-Type do cliente. JPEG, PNG, WEBP e PDF. HEIC é recusado de propósito.
 */
public final class AssinaturaArquivo {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP = {0x57, 0x45, 0x42, 0x50};
    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D};

    private AssinaturaArquivo() {
    }

    public static Optional<String> detectar(byte[] b) {
        if (b == null) {
            return Optional.empty();
        }
        if (comeca(b, 0, JPEG)) {
            return Optional.of("image/jpeg");
        }
        if (comeca(b, 0, PNG)) {
            return Optional.of("image/png");
        }
        if (comeca(b, 0, RIFF) && comeca(b, 8, WEBP)) {
            return Optional.of("image/webp");
        }
        if (comeca(b, 0, PDF)) {
            return Optional.of("application/pdf");
        }
        return Optional.empty();
    }

    private static boolean comeca(byte[] b, int deslocamento, byte[] prefixo) {
        if (b.length < deslocamento + prefixo.length) {
            return false;
        }
        for (int i = 0; i < prefixo.length; i++) {
            if (b[deslocamento + i] != prefixo[i]) {
                return false;
            }
        }
        return true;
    }
}
