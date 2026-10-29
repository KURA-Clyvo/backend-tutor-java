package br.com.clyvo.kura.tutor.laudo.infra;

import br.com.clyvo.kura.tutor.laudo.application.LaudoLeituraRepositorio;
import br.com.clyvo.kura.tutor.laudo.application.LaudoTutor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Leitura de {@code kura_docs.laudos} (contrato v1, dona .NET). Filtro e sort casam com o índice
 * {@code ix_laudo_pet_ativo_data} ({idPet, stAtivo, dataExame:-1}). A projeção é uma lista
 * POSITIVA: observacao, dados, idUsuarioAutor e idClinica nunca saem do banco.
 */
@Repository
public class LaudoMongoLeitura implements LaudoLeituraRepositorio {

    static final String COLECAO = "laudos";

    private final MongoTemplate mongo;

    public LaudoMongoLeitura(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<LaudoTutor> listarAtivos(long idPet) {
        return mongo.find(consulta(idPet), Document.class, COLECAO).stream().map(LaudoMongoLeitura::paraLaudo).toList();
    }

    /** A consulta exata enviada ao Mongo (filtro, sort, teto e projeção positiva); visível ao teste. */
    static Query consulta(long idPet) {
        Query query = new Query(Criteria.where("idPet").is(idPet).and("stAtivo").is(true))
                .with(Sort.by(Sort.Order.desc("dataExame")))
                .limit(TETO_LEITURA);
        query.fields().include("tipo", "titulo", "dataExame", "laboratorio",
                "arquivo.chave", "arquivo.contentType", "arquivo.tamanhoBytes");
        return query;
    }

    private static LaudoTutor paraLaudo(Document doc) {
        Document arquivo = doc.get("arquivo", Document.class);
        return new LaudoTutor(
                doc.getObjectId("_id").toHexString(),
                doc.getString("tipo"),
                doc.getString("titulo"),
                doc.getDate("dataExame").toInstant(),
                doc.getString("laboratorio"),
                arquivo.getString("contentType"),
                arquivo.get("tamanhoBytes", Number.class).longValue(),
                arquivo.getString("chave"));
    }
}
