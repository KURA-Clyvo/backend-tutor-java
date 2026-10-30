// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pedidos da Loja (pagamento e entrega simulados: o pedido nasce AGUARDANDO_PARCEIRO).
 *
 * <p>Criar é idempotente por {@code Idempotency-Key} (por tutor). Passos: (1) chave já usada
 * devolve o pedido; (2) lê o carrinho válido e o catálogo; (3) insere o snapshot; (4) corrida
 * da mesma chave ({@link DuplicateKeyException}, estrito) devolve o pedido existente; (5) apaga o
 * carrinho só se ninguém mexeu nele depois (versão = {@code atualizadoEm} lido no passo 2).
 * Proibido capturar {@code DataIntegrityViolationException}: o 121 do validator é irmão dela.
 */
@Service
public class PedidoLojaService {

    private static final Pattern FORMATO_UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final PedidoLojaRepositorio pedidos;
    private final CarrinhoRepositorio carrinhos;
    private final CatalogoRepositorio catalogo;
    private final Clock clock;

    public PedidoLojaService(PedidoLojaRepositorio pedidos, CarrinhoRepositorio carrinhos,
                             CatalogoRepositorio catalogo, Clock clock) {
        this.pedidos = pedidos;
        this.carrinhos = carrinhos;
        this.catalogo = catalogo;
        this.clock = clock;
    }

    public ResultadoPedido criar(long idTutor, String chaveBruta) {
        String chave = normalizarChave(chaveBruta);

        Optional<PedidoLoja> existente = pedidos.buscarPorChave(idTutor, chave);
        if (existente.isPresent()) {
            return repeticao(idTutor, existente.get());
        }

        Instant agora = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        CarrinhoSalvo carrinho = carrinhos.buscarValido(idTutor, agora.minus(LojaCarrinho.VALIDADE))
                .filter(c -> !c.itens().isEmpty())
                .orElseThrow(LojaException::carrinhoVazio);
        List<ItemPedido> itens = congelar(carrinho);
        long total = itens.stream().mapToLong(ItemPedido::subtotal).sum();

        PedidoLoja criado;
        try {
            criado = pedidos.inserir(idTutor, new NovoPedido(chave, itens, total, carrinho.atualizadoEm(), agora));
        } catch (DuplicateKeyException corrida) {
            PedidoLoja vencedor = pedidos.buscarPorChave(idTutor, chave).orElseThrow(() -> corrida);
            return repeticao(idTutor, vencedor);
        }
        carrinhos.apagarSeVersao(idTutor, carrinho.atualizadoEm());
        return new ResultadoPedido(criado, true);
    }

    public FatiaPedidos listar(long idTutor, int page, int size) {
        return pedidos.listar(idTutor, page, size);
    }

    public PedidoLoja detalhar(long idTutor, String idPedido) {
        return pedidos.buscar(idTutor, idPedido).orElseThrow(LojaException::pedidoNaoEncontrado);
    }

    /** Cancelamento lógico e idempotente: cancelar um pedido já cancelado devolve 200. */
    public PedidoLoja cancelar(long idTutor, String idPedido) {
        pedidos.cancelar(idTutor, idPedido, clock.instant().truncatedTo(ChronoUnit.MILLIS));
        return detalhar(idTutor, idPedido);
    }

    /** Repetição da chave: refaz o passo 5 com a versão guardada no pedido (fecha a falha entre o 3 e o 5). */
    private ResultadoPedido repeticao(long idTutor, PedidoLoja pedido) {
        carrinhos.apagarSeVersao(idTutor, pedido.carrinhoVersao());
        return new ResultadoPedido(pedido, false);
    }

    /** Relê o catálogo: item fora dele ou inativo recusa o pedido inteiro, sem gravar nada. */
    private List<ItemPedido> congelar(CarrinhoSalvo carrinho) {
        Map<String, ItemCatalogo> ativos = catalogo.buscarAtivosPorSlugs(carrinho.itens().keySet()).stream()
                .collect(Collectors.toMap(ItemCatalogo::slug, Function.identity()));
        List<String> indisponiveis = carrinho.itens().keySet().stream()
                .filter(slug -> !ativos.containsKey(slug)).toList();
        if (!indisponiveis.isEmpty()) {
            throw LojaException.itemIndisponivel(indisponiveis);
        }
        List<ItemPedido> itens = new ArrayList<>();
        carrinho.itens().forEach((slug, quantidade) -> {
            ItemCatalogo c = ativos.get(slug);
            itens.add(new ItemPedido(slug, c.nome(), c.categoria(), c.preco(), quantidade, c.exemplo()));
        });
        return itens;
    }

    private static String normalizarChave(String chave) {
        if (chave == null || !FORMATO_UUID.matcher(chave).matches()) {
            throw LojaException.idempotencyKeyInvalida();
        }
        return chave.toLowerCase(Locale.ROOT);
    }
}
