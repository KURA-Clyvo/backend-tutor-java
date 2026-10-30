// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import java.time.Instant;
import java.util.Optional;

/**
 * Porta do carrinho (chave-valor por tutor, {@code _id = idTutor}). TODO método recebe
 * {@code long idTutor}; a leitura do catálogo mora no {@link CatalogoRepositorio}, que é global.
 */
public interface CarrinhoRepositorio {

    /** Carrinho com {@code atualizadoEm >= limite}; vencido ou ausente = vazio. */
    Optional<CarrinhoSalvo> buscarValido(long idTutor, Instant limite);

    /** Apaga o carrinho vencido ({@code atualizadoEm < limite}), para a escrita não ressuscitar itens antigos. */
    void descartarVencido(long idTutor, Instant limite);

    /** Soma {@code quantidade} ao produto ({@code $inc}, atômico). Estouro do validator: {@link LimiteCarrinhoException}. */
    void somar(long idTutor, String slug, int quantidade, Instant agora);

    /** Define a quantidade absoluta do produto ({@code $set}, idempotente). */
    void definir(long idTutor, String slug, int quantidade, Instant agora);

    /** Remove o produto; produto ou carrinho ausente não é erro. */
    void remover(long idTutor, String slug, Instant agora);

    void esvaziar(long idTutor);

    /** Passo 5 do pedido: só apaga se ninguém mexeu no carrinho depois dele. Devolve se apagou. */
    boolean apagarSeVersao(long idTutor, Instant versao);
}
