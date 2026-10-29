package br.com.clyvo.kura.tutor.documento.application;

import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentoTutorServiceTest {

    private static final long TUTOR = 7L;
    private static final long PET = 11L;
    private static final Instant AGORA = Instant.parse("2026-10-29T15:00:00Z");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    private final DocumentoTutorRepositorio repositorio = mock(DocumentoTutorRepositorio.class);
    private final PetRepository petRepository = mock(PetRepository.class);
    private DocumentoTutorService service;

    @BeforeEach
    void preparar() {
        service = new DocumentoTutorService(repositorio, petRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private void petDoTutor() {
        when(petRepository.countVinculo(PET, TUTOR)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET, "S")).thenReturn(Optional.of(mock(Pet.class)));
    }

    private static DocumentoTutor gravado() {
        return new DocumentoTutor("abc", PET, TipoDocumento.RG_ANIMAL, null, null, "image/jpeg", 7, AGORA);
    }

    @Test
    @DisplayName("criar: confere o vínculo ANTES de qualquer chamada ao repositório de documentos")
    void vinculoAntesDoMongo() {
        petDoTutor();
        when(repositorio.contar(TUTOR, PET)).thenReturn(0L);
        when(repositorio.inserir(eq(TUTOR), any())).thenReturn(gravado());

        service.criar(TUTOR, PET, TipoDocumento.RG_ANIMAL, null, null, JPEG);

        InOrder ordem = inOrder(petRepository, repositorio);
        ordem.verify(petRepository).countVinculo(PET, TUTOR);
        ordem.verify(repositorio).contar(TUTOR, PET);
        ordem.verify(repositorio).inserir(eq(TUTOR), any());
    }

    @Test
    @DisplayName("sem vínculo: 403 e o repositório de documentos nunca é tocado (criar, listar, abrir, apagar)")
    void semVinculoNaoTocaORepositorio() {
        when(petRepository.countVinculo(PET, TUTOR)).thenReturn(0L);

        assertThatThrownBy(() -> service.criar(TUTOR, PET, TipoDocumento.RG_ANIMAL, null, null, JPEG))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.listar(TUTOR, PET)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.abrir(TUTOR, PET, "abc")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.apagar(TUTOR, PET, "abc")).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repositorio);
    }

    @Test
    @DisplayName("pet inativo: 404 sem tocar o repositório")
    void petInativo() {
        when(petRepository.countVinculo(PET, TUTOR)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET, "S")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(TUTOR, PET)).isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(repositorio);
    }

    @Test
    @DisplayName("o contentType gravado vem da assinatura e o sha256 é calculado no servidor")
    void contentTypeEShaDoServidor() {
        petDoTutor();
        when(repositorio.contar(TUTOR, PET)).thenReturn(0L);
        when(repositorio.inserir(eq(TUTOR), any())).thenReturn(gravado());

        service.criar(TUTOR, PET, TipoDocumento.MICROCHIP, EmissorDocumento.OUTRO, "985-112.34", JPEG);

        ArgumentCaptor<NovoDocumento> capturado = ArgumentCaptor.forClass(NovoDocumento.class);
        verify(repositorio).inserir(eq(TUTOR), capturado.capture());
        NovoDocumento novo = capturado.getValue();
        assertThat(novo.contentType()).isEqualTo("image/jpeg");
        assertThat(novo.sha256()).matches("^[0-9a-f]{64}$");
        assertThat(novo.criadoEm()).isEqualTo(AGORA);
        assertThat(novo.numero()).isEqualTo("985-112.34");
    }

    @Test
    @DisplayName("arquivo acima de 5 MiB: 413, sem gravar")
    void arquivoGrande() {
        petDoTutor();
        byte[] grande = new byte[(int) DocumentoTutorService.TAMANHO_MAXIMO_BYTES + 1];
        grande[0] = (byte) 0xFF;
        grande[1] = (byte) 0xD8;
        grande[2] = (byte) 0xFF;

        assertThatThrownBy(() -> service.criar(TUTOR, PET, TipoDocumento.RG_ANIMAL, null, null, grande))
                .isInstanceOf(ArquivoGrandeException.class);

        verify(repositorio, never()).inserir(anyLong(), any());
    }

    @Test
    @DisplayName("assinatura desconhecida: 415, sem gravar")
    void assinaturaDesconhecida() {
        petDoTutor();

        assertThatThrownBy(() -> service.criar(TUTOR, PET, TipoDocumento.RG_ANIMAL, null, null, "texto".getBytes()))
                .isInstanceOf(TipoArquivoNaoSuportadoException.class);

        verify(repositorio, never()).inserir(anyLong(), any());
    }

    @Test
    @DisplayName("arquivo vazio e número com espaço: 400")
    void validacoes() {
        petDoTutor();

        assertThatThrownBy(() -> service.criar(TUTOR, PET, TipoDocumento.RG_ANIMAL, null, null, new byte[0]))
                .isInstanceOf(DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.criar(TUTOR, PET, TipoDocumento.RG_ANIMAL, null, "12 34", JPEG))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    @DisplayName("20 documentos já gravados: 409 e nada é inserido")
    void teto() {
        petDoTutor();
        when(repositorio.contar(TUTOR, PET)).thenReturn(20L);

        assertThatThrownBy(() -> service.criar(TUTOR, PET, TipoDocumento.RG_ANIMAL, null, null, JPEG))
                .isInstanceOf(LimiteDocumentosException.class);

        verify(repositorio, never()).inserir(anyLong(), any());
    }

    @Test
    @DisplayName("abrir/apagar documento que não existe para o tutor: 404")
    void documentoInexistente() {
        petDoTutor();
        when(repositorio.abrirArquivo(TUTOR, PET, "x")).thenReturn(Optional.empty());
        when(repositorio.apagar(TUTOR, PET, "x")).thenReturn(false);

        assertThatThrownBy(() -> service.abrir(TUTOR, PET, "x")).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.apagar(TUTOR, PET, "x")).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("listar devolve o que o repositório do tutor devolve")
    void listar() {
        petDoTutor();
        when(repositorio.listar(TUTOR, PET)).thenReturn(List.of(gravado()));

        assertThat(service.listar(TUTOR, PET)).hasSize(1);
    }
}
