package br.com.clyvo.kura.tutor.laudo.application;

import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.pet.GuardaPetDoTutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LaudoTutorServiceTest {

    private static final long TUTOR = 7L;
    private static final long PET = 11L;

    private final LaudoLeituraRepositorio repositorio = mock(LaudoLeituraRepositorio.class);
    private final PetRepository petRepository = mock(PetRepository.class);
    private final LaudoTutorService service = new LaudoTutorService(repositorio, new GuardaPetDoTutor(petRepository));

    private static LaudoTutor laudo() {
        return new LaudoTutor("abc", "HEMOGRAMA", "Hemograma", Instant.parse("2026-10-01T10:00:00Z"), null,
                "application/pdf", 10, "clinica/1/pet/11/laudo/a.pdf");
    }

    @Test
    @DisplayName("listar: confere o vínculo ANTES de ler o repositório de laudos e devolve o que ele traz")
    void vinculoAntesDoMongo() {
        // Arrange
        when(petRepository.countVinculo(PET, TUTOR)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET, "S")).thenReturn(Optional.of(mock(Pet.class)));
        when(repositorio.listarAtivos(PET)).thenReturn(List.of(laudo()));

        // Act
        List<LaudoTutor> lista = service.listar(TUTOR, PET);

        // Assert
        InOrder ordem = inOrder(petRepository, repositorio);
        ordem.verify(petRepository).countVinculo(PET, TUTOR);
        ordem.verify(repositorio).listarAtivos(PET);
        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("listar sem vínculo: 403 e o repositório de laudos nunca é tocado")
    void semVinculoNaoTocaOMongo() {
        when(petRepository.countVinculo(PET, TUTOR)).thenReturn(0L);

        assertThatThrownBy(() -> service.listar(TUTOR, PET)).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repositorio);
    }

    @Test
    @DisplayName("listar com pet inativo: 404 e o repositório de laudos nunca é tocado")
    void petInativoNaoTocaOMongo() {
        when(petRepository.countVinculo(PET, TUTOR)).thenReturn(1L);
        when(petRepository.findByIdPetAndStAtivo(PET, "S")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(TUTOR, PET)).isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(repositorio);
    }
}
