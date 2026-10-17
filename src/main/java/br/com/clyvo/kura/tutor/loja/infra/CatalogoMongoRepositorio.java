// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.loja.application.CatalogoRepositorio;
import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.application.FatiaCatalogo;
import br.com.clyvo.kura.tutor.loja.application.ItemCatalogo;
import org.bson.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Leitura do catálogo no Mongo. Consulta = a dos índices do LOJA-01 (ESR): igualdade em
 * ativo (e categoria), ordenação por ordem, slug. {@code skip} calculado em long; o teto de
 * page (10000) e de size (50) é validado no controller e mantém skip dentro de int.
 */
@Slf4j
@Repository
public class CatalogoMongoRepositorio implements CatalogoRepositorio {

    private final MongoTemplate mongo;

    public CatalogoMongoRepositorio(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public FatiaCatalogo listarAtivos(CategoriaLoja categoria, int page, int size) {
        Query query = new Query(Criteria.where("ativo").is(true));
        if (categoria != null) {
            query.addCriteria(Criteria.where("categoria").is(categoria.name()));
        }
        query.with(Sort.by(Sort.Direction.ASC, "ordem", "slug"));
        query.skip((long) page * size).limit(size + 1);
        query.fields().exclude("_id")
                .include("slug", "nome", "descricao", "categoria", "preco", "moeda", "exemplo", "dados");

        List<Document> lidos = mongo.find(query, Document.class, SchemaCatalogoAplicador.COLECAO);
        boolean temProxima = lidos.size() > size;
        List<ItemCatalogo> itens = new ArrayList<>();
        for (Document doc : lidos.subList(0, Math.min(size, lidos.size()))) {
            paraItem(doc).ifPresentOrElse(itens::add, () -> log.warn(
                    "Loja: documento do catálogo inválido ignorado (slug={})", doc.get("slug")));
        }
        return new FatiaCatalogo(itens, temProxima);
    }

    /**
     * Documento sem preço ou com categoria fora do enum é dado corrompido (o validator strict os
     * impede; só mordem se ele for afrouxado): é pulado com WARN, nunca vira preço 0 nem 400.
     */
    static Optional<ItemCatalogo> paraItem(Document doc) {
        Number preco = doc.get("preco", Number.class);
        Optional<CategoriaLoja> categoria = CategoriaLoja.tentar(doc.getString("categoria"));
        if (preco == null || categoria.isEmpty()) {
            return Optional.empty();
        }
        Document dados = doc.get("dados", Document.class);
        Map<String, Object> mapaDados = dados == null ? Map.of() : new LinkedHashMap<>(dados);
        return Optional.of(new ItemCatalogo(
                doc.getString("slug"),
                doc.getString("nome"),
                doc.getString("descricao"),
                categoria.get(),
                preco.longValue(),
                doc.getString("moeda"),
                Boolean.TRUE.equals(doc.getBoolean("exemplo")),
                mapaDados));
    }
}
