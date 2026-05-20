package br.com.clyvo.kura.tutor.tutor.application;

import br.com.clyvo.kura.tutor.entity.Especie;
import br.com.clyvo.kura.tutor.repository.EspecieRepository;
import br.com.clyvo.kura.tutor.shared.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Testa que @Cacheable("especies") em EspecieService funciona corretamente:
 *   - primeira chamada bate no repositório
 *   - chamadas seguintes retornam do cache (repositório não é chamado novamente)
 *
 * Usa contexto Spring slim (sem @SpringBootTest) para validar o proxy de cache
 * sem levantar toda a aplicação.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EspecieService.class,
        CacheConfig.class,
        EspecieServiceCacheTest.TestConfig.class
})
class EspecieServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        EspecieRepository especieRepository() {
            return Mockito.mock(EspecieRepository.class);
        }
    }

    @Autowired EspecieService  especieService;
    @Autowired EspecieRepository especieRepository;
    @Autowired CacheManager    cacheManager;

    @BeforeEach
    void limparCacheEResetarMock() {
        cacheManager.getCache("especies").clear();
        reset(especieRepository);
    }

    // ─── primeiraChamadaBateNoRepo ────────────────────────────────────────────

    @Test
    @DisplayName("primeiraChamadaBateNoRepo")
    void primeiraChamadaBateNoRepo() {
        when(especieRepository.findAll()).thenReturn(List.of(especieFake(1L, "Cão")));

        especieService.listarTodas();

        verify(especieRepository, times(1)).findAll();
    }

    // ─── segundaChamadaVemDoCache ─────────────────────────────────────────────

    @Test
    @DisplayName("segundaChamadaVemDoCache")
    void segundaChamadaVemDoCache() {
        when(especieRepository.findAll()).thenReturn(List.of(especieFake(1L, "Cão")));

        especieService.listarTodas();
        especieService.listarTodas(); // deve ser servida do cache

        verify(especieRepository, times(1)).findAll(); // repo chamado UMA vez apenas
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Especie especieFake(Long id, String nome) {
        Especie e = mock(Especie.class);
        when(e.getIdEspecie()).thenReturn(id);
        when(e.getNmEspecie()).thenReturn(nome);
        return e;
    }
}
