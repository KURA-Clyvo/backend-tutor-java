// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.time.Instant;
import java.util.Optional;

/** Porta dos pedidos. TODO método recebe {@code long idTutor}: pedido de outro tutor é indistinguível de inexistente. */
public interface PedidoLojaRepositorio {

    Optional<PedidoLoja> buscarPorChave(long idTutor, String chaveIdempotencia);

    /** Insere; chave repetida lança {@code org.springframework.dao.DuplicateKeyException}. */
    PedidoLoja inserir(long idTutor, NovoPedido novo);

    FatiaPedidos listar(long idTutor, int page, int size);

    /** Id malformado ou de outro tutor = vazio. */
    Optional<PedidoLoja> buscar(long idTutor, String idPedido);

    /** AGUARDANDO_PARCEIRO para CANCELADO, atômico no documento; pedido já cancelado, alheio ou inexistente não muda nada. */
    void cancelar(long idTutor, String idPedido, Instant agora);
}
