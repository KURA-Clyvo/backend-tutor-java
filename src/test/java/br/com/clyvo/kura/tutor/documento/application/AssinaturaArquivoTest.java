package br.com.clyvo.kura.tutor.documento.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AssinaturaArquivoTest {

    private static byte[] bytes(int... valores) {
        byte[] b = new byte[valores.length];
        for (int i = 0; i < valores.length; i++) {
            b[i] = (byte) valores[i];
        }
        return b;
    }

    @Test
    @DisplayName("JPEG (FF D8 FF) é reconhecido")
    void jpeg() {
        assertThat(AssinaturaArquivo.detectar(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0x10))).contains("image/jpeg");
    }

    @Test
    @DisplayName("PNG (89 50 4E 47 0D 0A 1A 0A) é reconhecido")
    void png() {
        assertThat(AssinaturaArquivo.detectar(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)))
                .contains("image/png");
    }

    @Test
    @DisplayName("WEBP (RIFF....WEBP) é reconhecido; RIFF de outro tipo (WAVE) não")
    void webp() {
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
        byte[] wave = "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(AssinaturaArquivo.detectar(webp)).contains("image/webp");
        assertThat(AssinaturaArquivo.detectar(wave)).isEmpty();
    }

    @Test
    @DisplayName("PDF (%PDF-) é reconhecido")
    void pdf() {
        assertThat(AssinaturaArquivo.detectar("%PDF-1.7\n".getBytes(StandardCharsets.ISO_8859_1)))
                .contains("application/pdf");
    }

    @Test
    @DisplayName("texto renomeado .jpg não tem assinatura de imagem: recusa")
    void textoRenomeadoJpg() {
        Optional<String> tipo = AssinaturaArquivo.detectar(
                "isto e apenas texto, nome foto.jpg".getBytes(StandardCharsets.UTF_8));

        assertThat(tipo).isEmpty();
    }

    @Test
    @DisplayName("HEIC, vazio, curto demais e null são recusados")
    void recusados() {
        byte[] heic = bytes(0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63);

        assertThat(AssinaturaArquivo.detectar(heic)).isEmpty();
        assertThat(AssinaturaArquivo.detectar(new byte[0])).isEmpty();
        assertThat(AssinaturaArquivo.detectar(bytes(0xFF, 0xD8))).isEmpty();
        assertThat(AssinaturaArquivo.detectar(null)).isEmpty();
    }
}
