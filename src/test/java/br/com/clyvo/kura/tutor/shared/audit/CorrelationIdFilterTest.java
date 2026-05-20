package br.com.clyvo.kura.tutor.shared.audit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CorrelationIdFilter — no Spring context needed.
 *
 * Verifies header propagation, UUID generation, and mandatory MDC cleanup.
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("deveGerarCorrelationIdSeAusente — UUID gerado, header na resposta, MDC limpo após request")
    void deveGerarCorrelationIdSeAusente() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcDuranteRequest = {null};

        FilterChain chain = (req, res) ->
                mdcDuranteRequest[0] = MDC.get(CorrelationIdFilter.MDC_KEY);

        filter.doFilter(request, response, chain);

        // Response header deve ser preenchido com UUID gerado
        String headerResposta = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(headerResposta).isNotBlank();

        // MDC estava populado durante a execução da chain
        assertThat(mdcDuranteRequest[0]).isNotNull();
        assertThat(mdcDuranteRequest[0]).isEqualTo(headerResposta);

        // MDC deve estar limpo após o finally
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("devePropagárCorrelationIdExistente — valor enviado pelo cliente é ecoado na resposta")
    void devePropagárCorrelationIdExistente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "abc-123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcDuranteRequest = {null};
        FilterChain chain = (req, res) ->
                mdcDuranteRequest[0] = MDC.get(CorrelationIdFilter.MDC_KEY);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("abc-123");
        assertThat(mdcDuranteRequest[0]).isEqualTo("abc-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("deveLimparMdcMesmoComExcecaoNaChain — finally garante limpeza em caso de erro")
    void deveLimparMdcMesmoComExcecaoNaChain() {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chainComErro = (req, res) -> {
            throw new RuntimeException("Erro simulado");
        };

        try {
            filter.doFilter(request, response, chainComErro);
        } catch (Exception ignored) {
            // Exceção propagada — MDC ainda deve estar limpo
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
