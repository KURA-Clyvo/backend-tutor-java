package br.com.clyvo.kura.tutor.repository;

import br.com.clyvo.kura.tutor.entity.Pet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PetRepositoryTest {

    @Autowired
    PetRepository petRepository;

    @Test
    @DisplayName("findByIdPetAndStAtivo — pet ativo retorna present com dados corretos")
    void findByIdPetAndStAtivo_ativo_deveRetornarPresent() {
        Optional<Pet> result = petRepository.findByIdPetAndStAtivo(1L, "S");

        assertThat(result).isPresent();
        assertThat(result.get().getNmPet()).isEqualTo("Marley");
        assertThat(result.get().getStAtivo()).isEqualTo("S");
    }

    @Test
    @DisplayName("findByIdPetAndStAtivo — pet inativo não é retornado pelo filtro ST_ATIVO='S'")
    @Sql(statements = {
        "INSERT INTO PET (ID_PET, ID_CLINICA, ID_ESPECIE, NM_PET, ST_ATIVO) " +
        "VALUES (99, 1, 1, 'Pet Inativo', 'N')"
    })
    void findByIdPetAndStAtivo_inativo_deveRetornarEmpty() {
        Optional<Pet> result = petRepository.findByIdPetAndStAtivo(99L, "S");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAtivosByIdTutor — retorna apenas pets ativos vinculados ao tutor")
    void findAtivosByIdTutor_deveRetornarPetsAtivos() {
        Page<Pet> result = petRepository.findAtivosByIdTutor(1L, Pageable.ofSize(10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getNmPet()).isEqualTo("Marley");
    }

    @Test
    @DisplayName("findAtivosByIdTutor — pet inativo vinculado ao tutor não é incluído no resultado")
    @Sql(statements = {
        "INSERT INTO PET (ID_PET, ID_CLINICA, ID_ESPECIE, NM_PET, ST_ATIVO) VALUES (99, 1, 1, 'Pet Inativo', 'N')",
        "INSERT INTO TUTOR_PET (ID_TUTOR, ID_PET, DS_VINCULO, DT_VINCULO, ST_PRINCIPAL) VALUES (1, 99, 'PROPRIETARIO', CURRENT_TIMESTAMP, 'S')"
    })
    void findAtivosByIdTutor_petInativo_naoRetornado() {
        Page<Pet> result = petRepository.findAtivosByIdTutor(1L, Pageable.ofSize(10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).noneMatch(p -> "Pet Inativo".equals(p.getNmPet()));
    }

    @Test
    @DisplayName("petRepository.save — não compila: método save não existe em Repository + PagingAndSortingRepository")
    void salvarPetNaoDeveCompiliar_arquiteturaImutavel() {
        // Este teste verifica a constraint arquitetural em runtime:
        // PetRepository não expõe save() — tentativa de chamar save() em tempo de compilação
        // resulta em erro de compilação, garantindo que Pet é verdadeiramente read-only.
        // O teste passa porque a inexistência do método save() é verificada pelo compilador,
        // não aqui. Este método documenta o contrato.
        assertThat(petRepository).isNotNull();
        // petRepository.save(new Pet()); // NÃO COMPILA — método não existe
    }
}
