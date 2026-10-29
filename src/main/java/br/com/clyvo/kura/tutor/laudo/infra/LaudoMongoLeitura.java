package br.com.clyvo.kura.tutor.laudo.infra;

import br.com.clyvo.kura.tutor.laudo.application.LaudoLeituraRepositorio;
import br.com.clyvo.kura.tutor.laudo.application.LaudoTutor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Leitura de {@code kura_docs.laudos} (contrato v1, dona .NET). Filtro e sort casam com o índice
 * {@code ix_laudo_pet_ativo_data} ({idPet, stAtivo, dataExame:-1}). A projeção é uma lista
 * POSITIVA: observacao, dados, idUsuarioAutor e idClinica nunca saem do banco.
 */
@Repository
public class LaudoMongoLeitura implements LaudoLeituraRepositorio {

    private static final Logger log = LoggerFactory.getLogger(LaudoMongoLeitura.class);

    static final String COLECAO = "laudos";

    private final MongoTemplate mongo;

    public LaudoMongoLeitura(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<LaudoTutor> listarAtivos(long idPet) {
        List<LaudoTutor> lista = new ArrayList<>();
        for (Document doc : mongo.find(consulta(idPet), Document.class, COLECAO)) {
            LaudoTutor laudo = paraLaudo(doc);
            if (laudo != null) {
                lista.add(laudo);
            }
        }
        return lista;
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

    /** Documento malformado (fora do contrato) é pulado com 1 WARN que traz só o _id; não derruba a lista. */
    private static LaudoTutor paraLaudo(Document doc) {
        Object id = doc.get("_id");
        Object data = doc.get("dataExame");
        Object arq = doc.get("arquivo");
        if (!(id instanceof ObjectId oid) || !(data instanceof Date dataExame) || !(arq instanceof Document arquivo)
                || !(arquivo.get("tamanhoBytes") instanceof Number tamanho)) {
            log.warn("Laudo fora do contrato v1 ignorado (_id={})", id);
            return null;
        }
        return new LaudoTutor(
                oid.toHexString(),
                texto(doc.get("tipo")),
                texto(doc.get("titulo")),
                dataExame.toInstant(),
                texto(doc.get("laboratorio")),
                texto(arquivo.get("contentType")),
                tamanho.longValue(),
                texto(arquivo.get("chave")));
    }

    private static String texto(Object valor) {
        return valor instanceof String s ? s : null;
    }
}
