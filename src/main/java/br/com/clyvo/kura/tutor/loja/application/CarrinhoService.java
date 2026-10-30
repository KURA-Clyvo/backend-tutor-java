// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import br.com.clyvo.kura.tutor.loja.bff.api.dto.CarrinhoResponse;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.ItemCarrinhoResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carrinho do tutor. Preço, nome e imagem vêm do catálogo no momento da leitura (o carrinho só
 * guarda slug e quantidade). Carrinho com mais de 2 h sem mexer é lido como vazio, mesmo antes
 * de o monitor TTL do Mongo apagá-lo, e toda escrita descarta o vencido primeiro.
 */
@Service
public class CarrinhoService {

    private final CarrinhoRepositorio carrinhos;
    private final CatalogoRepositorio catalogo;
    private final Clock clock;

    public CarrinhoService(CarrinhoRepositorio carrinhos, CatalogoRepositorio catalogo, Clock clock) {
        this.carrinhos = carrinhos;
        this.catalogo = catalogo;
        this.clock = clock;
    }

    public CarrinhoResponse ver(long idTutor) {
        Instant limite = agora().minus(LojaCarrinho.VALIDADE);
        return montar(carrinhos.buscarValido(idTutor, limite));
    }

    public CarrinhoResponse adicionar(long idTutor, String slug, int quantidade) {
        exigirProdutoAtivo(slug);
        Instant agora = agora();
        carrinhos.descartarVencido(idTutor, agora.minus(LojaCarrinho.VALIDADE));
        carrinhos.somar(idTutor, slug, quantidade, agora);
        return ver(idTutor);
    }

    public CarrinhoResponse definir(long idTutor, String slug, int quantidade) {
        exigirProdutoAtivo(slug);
        Instant agora = agora();
        carrinhos.descartarVencido(idTutor, agora.minus(LojaCarrinho.VALIDADE));
        carrinhos.definir(idTutor, slug, quantidade, agora);
        return ver(idTutor);
    }

    /** Não consulta o catálogo: produto desativado tem de poder sair do carrinho. */
    public CarrinhoResponse remover(long idTutor, String slug) {
        Instant agora = agora();
        carrinhos.descartarVencido(idTutor, agora.minus(LojaCarrinho.VALIDADE));
        carrinhos.remover(idTutor, slug, agora);
        return ver(idTutor);
    }

    public void esvaziar(long idTutor) {
        carrinhos.esvaziar(idTutor);
    }

    /** Milissegundos, a precisão do Mongo: a versão comparada no passo 5 do pedido tem de ser igual à gravada. */
    private Instant agora() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }

    private void exigirProdutoAtivo(String slug) {
        if (catalogo.buscarAtivosPorSlugs(List.of(slug)).isEmpty()) {
            throw LojaException.produtoNaoEncontrado();
        }
    }

    private CarrinhoResponse montar(Optional<CarrinhoSalvo> salvo) {
        if (salvo.isEmpty() || salvo.get().itens().isEmpty()) {
            return new CarrinhoResponse(LojaAviso.TEXTO, List.of(), 0, "BRL", 0, 0, null);
        }
        Map<String, Integer> quantidades = salvo.get().itens();
        Map<String, ItemCatalogo> ativos = catalogo.buscarAtivosPorSlugs(quantidades.keySet()).stream()
                .collect(Collectors.toMap(ItemCatalogo::slug, Function.identity()));
        List<ItemCarrinhoResponse> itens = new ArrayList<>();
        long total = 0;
        int unidades = 0;
        for (Map.Entry<String, Integer> e : quantidades.entrySet()) {
            ItemCatalogo item = ativos.get(e.getKey());
            ItemCarrinhoResponse linha = ItemCarrinhoResponse.de(e.getKey(), e.getValue(), item);
            if (linha.subtotal() != null) {
                total += linha.subtotal();
            }
            unidades += e.getValue();
            itens.add(linha);
        }
        return new CarrinhoResponse(LojaAviso.TEXTO, itens, total, "BRL", itens.size(), unidades,
                salvo.get().atualizadoEm().plus(LojaCarrinho.VALIDADE));
    }
}
