// KURA-LOJA — carrinho e pedidos (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.loja.application.CarrinhoRepositorio;
import br.com.clyvo.kura.tutor.loja.application.CarrinhoSalvo;
import br.com.clyvo.kura.tutor.loja.application.LimiteCarrinhoException;
import com.mongodb.MongoException;
import org.bson.Document;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Carrinho no Mongo: um documento por tutor ({@code _id = idTutor}); cada operação mexe em um
 * documento só, portanto é atômica. O limite de 99 por produto e 50 produtos é do validator do
 * banco: o erro 121 chega como {@code DataIntegrityViolationException} e é traduzido aqui, pela
 * causa (código 121), nunca pela mensagem do servidor.
 */
@Repository
public class CarrinhoMongoRepositorio implements CarrinhoRepositorio {

    private static final String COLECAO = SchemaCarrinhoAplicador.COLECAO;
    private static final int VALIDACAO_RECUSADA = 121;

    private final MongoTemplate mongo;

    public CarrinhoMongoRepositorio(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<CarrinhoSalvo> buscarValido(long idTutor, Instant limite) {
        Query query = new Query(Criteria.where("_id").is(idTutor).and("atualizadoEm").gte(Date.from(limite)));
        Document doc = mongo.findOne(query, Document.class, COLECAO);
        if (doc == null) {
            return Optional.empty();
        }
        TreeMap<String, Integer> itens = new TreeMap<>();
        Document lidos = doc.get("itens", Document.class);
        if (lidos != null) {
            lidos.forEach((slug, qtd) -> itens.put(slug, ((Number) qtd).intValue()));
        }
        return Optional.of(new CarrinhoSalvo(itens, doc.getDate("atualizadoEm").toInstant()));
    }

    @Override
    public void descartarVencido(long idTutor, Instant limite) {
        mongo.remove(new Query(Criteria.where("_id").is(idTutor).and("atualizadoEm").lt(Date.from(limite))), COLECAO);
    }

    @Override
    public void somar(long idTutor, String slug, int quantidade, Instant agora) {
        gravar(idTutor, upsert(agora).inc(campo(slug), quantidade));
    }

    @Override
    public void definir(long idTutor, String slug, int quantidade, Instant agora) {
        gravar(idTutor, upsert(agora).set(campo(slug), quantidade));
    }

    @Override
    public void remover(long idTutor, String slug, Instant agora) {
        mongo.updateFirst(porTutor(idTutor),
                new Update().unset(campo(slug)).set("atualizadoEm", Date.from(agora)), COLECAO);
    }

    @Override
    public void esvaziar(long idTutor) {
        mongo.remove(porTutor(idTutor), COLECAO);
    }

    @Override
    public void apagarSeVersao(long idTutor, Instant versao) {
        Query query = new Query(Criteria.where("_id").is(idTutor).and("atualizadoEm").is(Date.from(versao)));
        mongo.remove(query, COLECAO);
    }

    private static Update upsert(Instant agora) {
        return new Update().set("atualizadoEm", Date.from(agora)).setOnInsert("criadoEm", Date.from(agora));
    }

    private void gravar(long idTutor, Update update) {
        try {
            mongo.upsert(porTutor(idTutor), update, COLECAO);
        } catch (DataIntegrityViolationException e) {
            if (recusadoPeloValidator(e)) {
                throw new LimiteCarrinhoException();
            }
            throw e;
        }
    }

    /** Procura o código 121 na cadeia de causas: é o único sinal estável (a mensagem do servidor leva o idTutor). */
    private static boolean recusadoPeloValidator(Throwable erro) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t instanceof MongoException m && m.getCode() == VALIDACAO_RECUSADA) {
                return true;
            }
        }
        return false;
    }

    private static Query porTutor(long idTutor) {
        return new Query(Criteria.where("_id").is(idTutor));
    }

    private static String campo(String slug) {
        return "itens." + slug;
    }
}
