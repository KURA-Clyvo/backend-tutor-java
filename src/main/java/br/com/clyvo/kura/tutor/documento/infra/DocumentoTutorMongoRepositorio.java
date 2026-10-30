package br.com.clyvo.kura.tutor.documento.infra;

import br.com.clyvo.kura.tutor.documento.application.ArquivoDocumento;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutor;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutorRepositorio;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutorService;
import br.com.clyvo.kura.tutor.documento.application.EmissorDocumento;
import br.com.clyvo.kura.tutor.documento.application.NovoDocumento;
import br.com.clyvo.kura.tutor.documento.application.TipoDocumento;
import br.com.clyvo.kura.tutor.shared.mongo.IdsMongo;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * documentos_tutor no Mongo (contrato v1). TODO filtro inclui {@code idTutor}: documento de outro
 * tutor é indistinguível de documento inexistente. Listas projetam fora {@code arquivo.conteudo}.
 */
@Repository
public class DocumentoTutorMongoRepositorio implements DocumentoTutorRepositorio {

    private static final String COLECAO = SchemaDocumentosAplicador.COLECAO;

    private final MongoTemplate mongo;

    public DocumentoTutorMongoRepositorio(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public DocumentoTutor inserir(long idTutor, NovoDocumento novo) {
        Document arquivo = new Document("conteudo", new Binary(novo.conteudo()))
                .append("contentType", novo.contentType())
                .append("tamanhoBytes", (long) novo.conteudo().length)
                .append("sha256", novo.sha256());
        Document doc = new Document("idTutor", idTutor)
                .append("idPet", novo.idPet())
                .append("tipo", novo.tipo().name());
        if (novo.emissor() != null) {
            doc.append("emissor", novo.emissor().name());
        }
        if (novo.numero() != null) {
            doc.append("numero", novo.numero());
        }
        doc.append("arquivo", arquivo).append("criadoEm", Date.from(novo.criadoEm()));
        Document gravado = mongo.insert(doc, COLECAO);
        return paraMetadados(gravado);
    }

    @Override
    public List<DocumentoTutor> listar(long idTutor, long idPet) {
        Query query = new Query(Criteria.where("idTutor").is(idTutor).and("idPet").is(idPet))
                .with(Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("_id")))
                .limit(DocumentoTutorService.TETO_POR_PET);
        query.fields().exclude("arquivo.conteudo");
        return mongo.find(query, Document.class, COLECAO).stream()
                .map(DocumentoTutorMongoRepositorio::paraMetadados)
                .toList();
    }

    @Override
    public long contar(long idTutor, long idPet) {
        return mongo.count(new Query(Criteria.where("idTutor").is(idTutor).and("idPet").is(idPet)), COLECAO);
    }

    @Override
    public Optional<ArquivoDocumento> abrirArquivo(long idTutor, long idPet, String idDocumento) {
        Optional<ObjectId> id = IdsMongo.objectId(idDocumento);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        Document doc = mongo.findOne(escopo(idTutor, idPet, id.get()), Document.class, COLECAO);
        if (doc == null) {
            return Optional.empty();
        }
        Document arquivo = doc.get("arquivo", Document.class);
        Binary conteudo = arquivo.get("conteudo", Binary.class);
        return Optional.of(new ArquivoDocumento(arquivo.getString("contentType"), conteudo.getData()));
    }

    @Override
    public boolean apagar(long idTutor, long idPet, String idDocumento) {
        Optional<ObjectId> id = IdsMongo.objectId(idDocumento);
        if (id.isEmpty()) {
            return false;
        }
        return mongo.remove(escopo(idTutor, idPet, id.get()), COLECAO).getDeletedCount() > 0;
    }

    private static Query escopo(long idTutor, long idPet, ObjectId id) {
        return new Query(Criteria.where("_id").is(id).and("idTutor").is(idTutor).and("idPet").is(idPet));
    }

    private static DocumentoTutor paraMetadados(Document doc) {
        Document arquivo = doc.get("arquivo", Document.class);
        String emissor = doc.getString("emissor");
        return new DocumentoTutor(
                doc.getObjectId("_id").toHexString(),
                doc.get("idPet", Number.class).longValue(),
                TipoDocumento.valueOf(doc.getString("tipo")),
                emissor == null ? null : EmissorDocumento.valueOf(emissor),
                doc.getString("numero"),
                arquivo.getString("contentType"),
                arquivo.get("tamanhoBytes", Number.class).longValue(),
                doc.getDate("criadoEm").toInstant());
    }
}
