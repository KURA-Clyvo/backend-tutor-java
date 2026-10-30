// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.application.FatiaPedidos;
import br.com.clyvo.kura.tutor.loja.application.ItemPedido;
import br.com.clyvo.kura.tutor.loja.application.NovoPedido;
import br.com.clyvo.kura.tutor.loja.application.PedidoLoja;
import br.com.clyvo.kura.tutor.loja.application.PedidoLojaRepositorio;
import br.com.clyvo.kura.tutor.loja.application.StatusPedido;
import br.com.clyvo.kura.tutor.shared.mongo.IdsMongo;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * pedidos_loja no Mongo. TODO filtro inclui {@code idTutor}: pedido de outro tutor é
 * indistinguível de pedido inexistente. O cancelamento é lógico e atômico no documento.
 */
@Repository
public class PedidoLojaMongoRepositorio implements PedidoLojaRepositorio {

    private static final String COLECAO = SchemaPedidosAplicador.COLECAO;

    private final MongoTemplate mongo;

    public PedidoLojaMongoRepositorio(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<PedidoLoja> buscarPorChave(long idTutor, String chaveIdempotencia) {
        Query query = new Query(Criteria.where("idTutor").is(idTutor).and("chaveIdempotencia").is(chaveIdempotencia));
        return Optional.ofNullable(mongo.findOne(query, Document.class, COLECAO)).map(PedidoLojaMongoRepositorio::paraPedido);
    }

    @Override
    public PedidoLoja inserir(long idTutor, NovoPedido novo) {
        List<Document> itens = new ArrayList<>();
        for (ItemPedido i : novo.itens()) {
            itens.add(new Document("slug", i.slug()).append("nome", i.nome())
                    .append("categoria", i.categoria().name()).append("precoUnitario", i.precoUnitario())
                    .append("quantidade", i.quantidade()).append("exemplo", i.exemplo()));
        }
        Document doc = new Document("idTutor", idTutor)
                .append("chaveIdempotencia", novo.chaveIdempotencia())
                .append("status", StatusPedido.AGUARDANDO_PARCEIRO.name())
                .append("itens", itens)
                .append("total", novo.total())
                .append("moeda", "BRL")
                .append("carrinhoVersao", Date.from(novo.carrinhoVersao()))
                .append("criadoEm", Date.from(novo.agora()))
                .append("atualizadoEm", Date.from(novo.agora()));
        return paraPedido(mongo.insert(doc, COLECAO));
    }

    @Override
    public FatiaPedidos listar(long idTutor, int page, int size) {
        Query query = new Query(Criteria.where("idTutor").is(idTutor))
                .with(Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("_id")))
                .skip((long) page * size).limit(size + 1);
        List<Document> lidos = mongo.find(query, Document.class, COLECAO);
        List<PedidoLoja> pedidos = lidos.subList(0, Math.min(size, lidos.size())).stream()
                .map(PedidoLojaMongoRepositorio::paraPedido).toList();
        return new FatiaPedidos(pedidos, lidos.size() > size);
    }

    @Override
    public Optional<PedidoLoja> buscar(long idTutor, String idPedido) {
        return IdsMongo.objectId(idPedido)
                .map(id -> mongo.findOne(escopo(idTutor, id), Document.class, COLECAO))
                .map(PedidoLojaMongoRepositorio::paraPedido);
    }

    @Override
    public void cancelar(long idTutor, String idPedido, Instant agora) {
        Optional<ObjectId> id = IdsMongo.objectId(idPedido);
        if (id.isEmpty()) {
            return;
        }
        Query query = escopo(idTutor, id.get()).addCriteria(Criteria.where("status").is(StatusPedido.AGUARDANDO_PARCEIRO.name()));
        Update update = new Update().set("status", StatusPedido.CANCELADO.name())
                .set("canceladoEm", Date.from(agora)).set("atualizadoEm", Date.from(agora));
        mongo.updateFirst(query, update, COLECAO);
    }

    private static Query escopo(long idTutor, ObjectId id) {
        return new Query(Criteria.where("_id").is(id).and("idTutor").is(idTutor));
    }

    private static PedidoLoja paraPedido(Document doc) {
        List<ItemPedido> itens = doc.getList("itens", Document.class).stream()
                .map(i -> new ItemPedido(i.getString("slug"), i.getString("nome"),
                        CategoriaLoja.valueOf(i.getString("categoria")),
                        i.get("precoUnitario", Number.class).longValue(),
                        i.get("quantidade", Number.class).intValue(),
                        Boolean.TRUE.equals(i.getBoolean("exemplo"))))
                .toList();
        Date cancelado = doc.getDate("canceladoEm");
        return new PedidoLoja(
                doc.getObjectId("_id").toHexString(),
                StatusPedido.valueOf(doc.getString("status")),
                itens,
                doc.get("total", Number.class).longValue(),
                doc.getDate("carrinhoVersao").toInstant(),
                doc.getDate("criadoEm").toInstant(),
                cancelado == null ? null : cancelado.toInstant());
    }
}
