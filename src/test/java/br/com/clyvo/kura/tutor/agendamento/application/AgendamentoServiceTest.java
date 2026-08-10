package br.com.clyvo.kura.tutor.agendamento.application;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoRequest;
import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.repository.AgendamentoRepository;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.Clinica;
import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.entity.TutorPet;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TASK-74 (metade Java): prova de mordida para a derivação de clínica em
 * AgendamentoService.criar.
 *
 * Contexto: o app do tutor não envia idClinica (nenhum DTO de pet do BFF o expõe),
 * então o servidor passa a derivar a clínica do pet em vez de exigi-la no request.
 * Quando idClinica vier preenchido e divergir da clínica real do pet, a requisição
 * é rejeitada (integridade cross-tenant) em vez de aceita em silêncio.
 */
@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock AgendamentoRepository agendamentoRepository;
    @Mock ContaTutorRepository  contaTutorRepository;
    @Mock TutorRepository       tutorRepository;
    @Mock PetRepository         petRepository;

    // Mantido mesmo após a TASK-74 remover EntityManager do construtor do service:
    // deixa este arquivo de teste executável tanto contra o código anterior (construtor
    // de 5 parâmetros, usado na rodada "antes" da prova de mordida) quanto contra o
    // atual (4 parâmetros) — Mockito simplesmente ignora mocks que o construtor não usa.
    @Mock EntityManager entityManager;

    @InjectMocks AgendamentoService service;

    private static final String EMAIL      = "tutor@clyvo.vet";
    private static final Long   ID_TUTOR    = 1L;
    private static final Long   ID_PET      = 10L;
    private static final Long   ID_CLINICA_DO_PET = 5L;
    private static final Long   ID_CLINICA_OUTRA   = 999L;

    // ─── (a) POST sem idClinica: hoje dá 400 (idClinica @NotNull), depois de fix cria ──

    @Test
    @DisplayName("criar sem idClinica — deriva a clínica do pet e cria o agendamento")
    void criarSemIdClinica_derivaClinicaDoPet() {
        Tutor tutor = mockTutor(ID_TUTOR);
        Clinica clinicaDoPet = mockClinica(ID_CLINICA_DO_PET);
        Pet pet = mockPetVinculadoAoTutor(ID_PET, ID_TUTOR, clinicaDoPet);

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(tutorRepository.findByIdTutorAndStAtivo(ID_TUTOR, "S")).thenReturn(Optional.of(tutor));
        when(petRepository.findByIdPetAndStAtivo(ID_PET, "S")).thenReturn(Optional.of(pet));
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgendamentoRequest request = new AgendamentoRequest(
                ID_PET, null, null, LocalDateTime.now().plusDays(7), "CONSULTA", 30, null);

        var response = service.criar(EMAIL, request);

        assertThat(response.idClinica()).isEqualTo(ID_CLINICA_DO_PET);
    }

    // ─── (b) POST com idClinica divergente da clínica do pet: hoje seria aceito, ──
    // ─── depois de fix é rejeitado explicitamente ────────────────────────────────

    @Test
    @DisplayName("criar com idClinica divergente da clínica do pet — rejeita com ForbiddenException")
    void criarComIdClinicaDivergente_rejeitaComForbidden() {
        Tutor tutor = mockTutor(ID_TUTOR);
        Clinica clinicaDoPet = mockClinica(ID_CLINICA_DO_PET);
        Pet pet = mockPetVinculadoAoTutor(ID_PET, ID_TUTOR, clinicaDoPet);

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(tutorRepository.findByIdTutorAndStAtivo(ID_TUTOR, "S")).thenReturn(Optional.of(tutor));
        when(petRepository.findByIdPetAndStAtivo(ID_PET, "S")).thenReturn(Optional.of(pet));

        AgendamentoRequest request = new AgendamentoRequest(
                ID_PET, ID_CLINICA_OUTRA, null, LocalDateTime.now().plusDays(7), "CONSULTA", 30, null);

        assertThatThrownBy(() -> service.criar(EMAIL, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("não corresponde à clínica do pet");
    }

    // ─── caso de controle: idClinica preenchido e correto continua funcionando ────

    @Test
    @DisplayName("criar com idClinica igual à clínica do pet — cria normalmente")
    void criarComIdClinicaCorreta_criaNormalmente() {
        Tutor tutor = mockTutor(ID_TUTOR);
        Clinica clinicaDoPet = mockClinica(ID_CLINICA_DO_PET);
        Pet pet = mockPetVinculadoAoTutor(ID_PET, ID_TUTOR, clinicaDoPet);

        when(contaTutorRepository.findIdTutorByEmail(EMAIL)).thenReturn(Optional.of(ID_TUTOR));
        when(tutorRepository.findByIdTutorAndStAtivo(ID_TUTOR, "S")).thenReturn(Optional.of(tutor));
        when(petRepository.findByIdPetAndStAtivo(ID_PET, "S")).thenReturn(Optional.of(pet));
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgendamentoRequest request = new AgendamentoRequest(
                ID_PET, ID_CLINICA_DO_PET, null, LocalDateTime.now().plusDays(7), "CONSULTA", 30, null);

        var response = service.criar(EMAIL, request);

        assertThat(response.idClinica()).isEqualTo(ID_CLINICA_DO_PET);
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private Tutor mockTutor(Long idTutor) {
        Tutor tutor = mock(Tutor.class);
        // lenient: só é lido em AgendamentoResponse.fromEntity, que o teste de
        // rejeição por idClinica divergente nunca alcança (a exceção interrompe antes) —
        // stubbing legitimamente não-usado nesse caso, não um descuido.
        lenient().when(tutor.getIdTutor()).thenReturn(idTutor);
        return tutor;
    }

    private Clinica mockClinica(Long idClinica) {
        Clinica clinica = mock(Clinica.class);
        when(clinica.getIdClinica()).thenReturn(idClinica);
        return clinica;
    }

    private Pet mockPetVinculadoAoTutor(Long idPet, Long idTutor, Clinica clinica) {
        Tutor tutorDoVinculo = mock(Tutor.class);
        when(tutorDoVinculo.getIdTutor()).thenReturn(idTutor);

        TutorPet vinculo = mock(TutorPet.class);
        when(vinculo.getTutor()).thenReturn(tutorDoVinculo);

        Pet pet = mock(Pet.class);
        // lenient: mesma razão do tutor acima — só lido em fromEntity.
        lenient().when(pet.getIdPet()).thenReturn(idPet);
        when(pet.getClinica()).thenReturn(clinica);
        when(pet.getTutorPets()).thenReturn(List.of(vinculo));
        return pet;
    }
}
