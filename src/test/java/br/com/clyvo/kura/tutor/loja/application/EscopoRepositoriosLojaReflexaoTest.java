package br.com.clyvo.kura.tutor.loja.application;

import br.com.clyvo.kura.tutor.documento.application.EscopoRepositorioReflexaoTest;
import br.com.clyvo.kura.tutor.loja.infra.CarrinhoMongoRepositorio;
import br.com.clyvo.kura.tutor.loja.infra.PedidoLojaMongoRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Estende às portas da Loja o teste de reflexão dos documentos: todo método público de
 * {@link CarrinhoRepositorio} e {@link PedidoLojaRepositorio} (e das interfaces que herdam)
 * recebe {@code long idTutor} primeiro. O catálogo é global (D-LOJA-12) e fica de fora de propósito.
 */
class EscopoRepositoriosLojaReflexaoTest {

    @Test
    @DisplayName("carrinho e pedidos, nas portas e nas implementações Mongo: todo método recebe long idTutor primeiro")
    void portasDeDadoDoTutorTemEscopo() {
        assertThat(EscopoRepositorioReflexaoTest.metodosSemIdTutor(CarrinhoRepositorio.class)).isEmpty();
        assertThat(EscopoRepositorioReflexaoTest.metodosSemIdTutor(CarrinhoMongoRepositorio.class)).isEmpty();
        assertThat(EscopoRepositorioReflexaoTest.metodosSemIdTutor(PedidoLojaRepositorio.class)).isEmpty();
        assertThat(EscopoRepositorioReflexaoTest.metodosSemIdTutor(PedidoLojaMongoRepositorio.class)).isEmpty();
    }

    @Test
    @DisplayName("controle positivo: o catálogo (global) é acusado pelo verificador, então o verificador enxerga as portas da Loja")
    void controlePositivoNoCatalogo() {
        assertThat(EscopoRepositorioReflexaoTest.metodosSemIdTutor(CatalogoRepositorio.class))
                .contains("CatalogoRepositorio.listarAtivos", "CatalogoRepositorio.buscarAtivosPorSlugs");
    }
}
