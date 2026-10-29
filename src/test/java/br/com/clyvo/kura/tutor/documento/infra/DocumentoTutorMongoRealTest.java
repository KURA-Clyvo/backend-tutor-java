package br.com.clyvo.kura.tutor.documento.infra;

import br.com.clyvo.kura.tutor.documento.application.ArquivoDocumento;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutor;
import br.com.clyvo.kura.tutor.documento.application.EmissorDocumento;
import br.com.clyvo.kura.tutor.documento.application.NovoDocumento;
import br.com.clyvo.kura.tutor.documento.application.TipoDocumento;
import br.com.clyvo.kura.tutor.shared.mongo.MongodEmbarcado;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Prova contra um mongod 7.0.x REAL embarcado (flapdoodle 4.20.0): o $jsonSchema do arquivo
 * versionado, os índices pelo aplicador de produção, o repositório concreto com dois tutores e o E61.
 */
@Tag("mongo-real")
@ExtendWith(OutputCaptureExtension.class)
class DocumentoTutorMongoRealTest {

    private static MongoClient cliente;

    private MongoDatabase database;
    private MongoTemplate template;
    private DocumentoTutorMongoRepositorio repositorio;
    private MongoCollection<Document> colecao;

    @BeforeAll
    static void sobeMongod() {
        cliente = MongoClients.create(MongodEmbarcado.uri());
    }

    @AfterAll
    static void fecha() {
        cliente.close();
    }

    @BeforeEach
    void bancoNovo() {
        String nome = "kura_docs_" + UUID.randomUUID().toString().replace("-", "");
        database = cliente.getDatabase(nome);
        template = spy(new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, nome)));
        repositorio = new DocumentoTutorMongoRepositorio(template);
        new SchemaDocumentosAplicador(database).aplicar();
        colecao = database.getCollection("documentos_tutor");
    }

    private static Document arquivoValido() {
        return new Document("conteudo", new Binary(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1}))
                .append("contentType", "image/jpeg")
                .append("tamanhoBytes", 4L)
                .append("sha256", "a".repeat(64));
    }

    private static Document docValido() {
        return new Document("idTutor", 1L).append("idPet", 10L).append("tipo", "RG_ANIMAL")
                .append("arquivo", arquivoValido()).append("criadoEm", new Date());
    }

    private static NovoDocumento novo(long idPet, Instant em) {
        return new NovoDocumento(idPet, TipoDocumento.RG_ANIMAL, EmissorDocumento.SINPATINHAS, "ABC-123",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 9, 9}, "image/jpeg", "b".repeat(64), em);
    }

    private void recusa(Document doc) {
        assertThatThrownBy(() -> colecao.insertOne(doc))
                .isInstanceOfSatisfying(MongoWriteException.class, e -> assertThat(e.getCode()).isEqualTo(121));
    }

    @Test
    @DisplayName("controle positivo: documento válido do contrato é aceito pelo schema")
    void documentoValidoEAceito() {
        colecao.insertOne(docValido());

        assertThat(colecao.countDocuments()).isEqualTo(1);
    }

    @Test
    @DisplayName("schema recusa campo extra na raiz, _class e campo extra em arquivo")
    void schemaRecusaCamposExtras() {
        recusa(docValido().append("observacao", "texto livre"));
        recusa(docValido().append("_class", "br.com.clyvo.Documento"));
        Document comExtra = docValido();
        comExtra.get("arquivo", Document.class).append("nomeOriginal", "rg-do-joao.jpg");
        recusa(comExtra);
    }

    @Test
    @DisplayName("schema recusa contentType fora do enum, tamanho acima de 5242880, sha256 e número fora do padrão")
    void schemaRecusaValoresInvalidos() {
        Document tipoErrado = docValido();
        tipoErrado.get("arquivo", Document.class).put("contentType", "image/heic");
        recusa(tipoErrado);

        Document grande = docValido();
        grande.get("arquivo", Document.class).put("tamanhoBytes", 5242881L);
        recusa(grande);

        Document noLimite = docValido();
        noLimite.get("arquivo", Document.class).put("tamanhoBytes", 5242880L);
        colecao.insertOne(noLimite);

        Document shaCurto = docValido();
        shaCurto.get("arquivo", Document.class).put("sha256", "abc");
        recusa(shaCurto);

        recusa(docValido().append("numero", "com espaco"));
        recusa(docValido().append("tipo", "CPF"));
        recusa(docValido().append("emissor", "CARTORIO"));
    }

    @Test
    @DisplayName("schema exige long: idTutor int32 é recusado")
    void schemaExigeLong() {
        Document intId = docValido();
        intId.put("idTutor", 1);
        recusa(intId);
    }

    @Test
    @DisplayName("os 2 índices existem com os nomes do contrato e as chaves do arquivo")
    void indicesCriadosComNome() {
        List<Document> indices = colecao.listIndexes().into(new ArrayList<>());

        assertThat(indices).extracting(i -> i.getString("name"))
                .contains("ix_doc_tutor_pet_criado", "ix_doc_pet_criado");
        Document principal = indices.stream().filter(i -> "ix_doc_tutor_pet_criado".equals(i.getString("name")))
                .findFirst().orElseThrow();
        assertThat(((Document) principal.get("key")).keySet()).containsExactly("idTutor", "idPet", "criadoEm", "_id");
    }

    @Test
    @DisplayName("repositório: grava pelo schema real, lista sem os bytes e devolve o arquivo com o contentType gravado")
    void gravaListaEAbre() {
        DocumentoTutor gravado = repositorio.inserir(1L, novo(10L, Instant.parse("2026-10-29T10:00:00Z")));

        assertThat(gravado.id()).hasSize(24);
        assertThat(gravado.tamanhoBytes()).isEqualTo(5);
        List<DocumentoTutor> lista = repositorio.listar(1L, 10L);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).numero()).isEqualTo("ABC-123");
        ArgumentCaptor<Query> consulta = ArgumentCaptor.forClass(Query.class);
        verify(template).find(consulta.capture(), eq(Document.class), eq("documentos_tutor"));
        assertThat(consulta.getValue().getFieldsObject().get("arquivo.conteudo")).isEqualTo(0);
        ArquivoDocumento arquivo = repositorio.abrirArquivo(1L, 10L, gravado.id()).orElseThrow();
        assertThat(arquivo.contentType()).isEqualTo("image/jpeg");
        assertThat(arquivo.conteudo()).containsExactly((byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 9, 9);
        assertThat(repositorio.contar(1L, 10L)).isEqualTo(1);
    }

    @Test
    @DisplayName("lista ordenada do mais novo para o mais antigo")
    void ordemDecrescente() {
        DocumentoTutor antigo = repositorio.inserir(1L, novo(10L, Instant.parse("2026-10-01T10:00:00Z")));
        DocumentoTutor recente = repositorio.inserir(1L, novo(10L, Instant.parse("2026-10-28T10:00:00Z")));

        assertThat(repositorio.listar(1L, 10L)).extracting(DocumentoTutor::id)
                .containsExactly(recente.id(), antigo.id());
    }

    @Test
    @DisplayName("dois tutores: o documento do A é invisível ao B ao listar, abrir e apagar; apagar pelo B remove 0")
    void doisTutores() {
        DocumentoTutor doA = repositorio.inserir(1L, novo(10L, Instant.parse("2026-10-29T10:00:00Z")));

        assertThat(repositorio.listar(2L, 10L)).isEmpty();
        assertThat(repositorio.contar(2L, 10L)).isZero();
        assertThat(repositorio.abrirArquivo(2L, 10L, doA.id())).isEmpty();
        assertThat(repositorio.apagar(2L, 10L, doA.id())).isFalse();
        assertThat(colecao.countDocuments()).isEqualTo(1);
        assertThat(repositorio.listar(1L, 10L)).hasSize(1);
        assertThat(repositorio.abrirArquivo(1L, 10L, doA.id())).isPresent();
    }

    @Test
    @DisplayName("apagar pelo dono remove fisicamente; id malformado é 'não existe'")
    void apagarFisico() {
        DocumentoTutor doc = repositorio.inserir(1L, novo(10L, Instant.parse("2026-10-29T10:00:00Z")));

        assertThat(repositorio.apagar(1L, 10L, "nao-e-objectid")).isFalse();
        assertThat(repositorio.apagar(1L, 10L, doc.id())).isTrue();
        assertThat(colecao.countDocuments()).isZero();
    }

    @Test
    @DisplayName("E61: mesmo nome com chave diferente (código 86) é removido e recriado conforme o arquivo")
    void e61MesmoNomeChaveDiferente(CapturedOutput saida) {
        colecao.dropIndex("ix_doc_pet_criado");
        colecao.createIndex(BsonDocument.parse("{\"idPet\": 1}"), new IndexOptions().name("ix_doc_pet_criado"));

        new SchemaDocumentosAplicador(database).aplicar();

        assertThat(chaveDe("ix_doc_pet_criado")).containsExactly("idPet", "criadoEm");
        assertThat(saida.getAll()).contains("Documentos: índice ix_doc_pet_criado divergente do arquivo (código 86)")
                .doesNotContain("mongodb://");
    }

    @Test
    @DisplayName("E61: mesma chave com opção diferente (unique) é removido e recriado sem unique")
    void e61MesmaChaveOpcaoDiferente() {
        colecao.dropIndex("ix_doc_pet_criado");
        colecao.createIndex(BsonDocument.parse("{\"idPet\": 1, \"criadoEm\": -1}"),
                new IndexOptions().name("ix_doc_pet_criado").unique(true));

        new SchemaDocumentosAplicador(database).aplicar();

        Document indice = colecao.listIndexes().into(new ArrayList<>()).stream()
                .filter(i -> "ix_doc_pet_criado".equals(i.getString("name"))).findFirst().orElseThrow();
        assertThat(indice.get("unique")).isNull();
        assertThat(((Document) indice.get("key")).keySet()).containsExactly("idPet", "criadoEm");
    }

    @Test
    @DisplayName("E61: mesma chave com OUTRO nome (código 85) remove o índice citado pelo servidor e cria o do arquivo")
    void e61MesmaChaveOutroNome(CapturedOutput saida) {
        colecao.dropIndex("ix_doc_pet_criado");
        colecao.createIndex(BsonDocument.parse("{\"idPet\": 1, \"criadoEm\": -1}"),
                new IndexOptions().name("residuo_antigo"));

        new SchemaDocumentosAplicador(database).aplicar();

        List<String> nomes = colecao.listIndexes().into(new ArrayList<>()).stream()
                .map(i -> i.getString("name")).toList();
        assertThat(nomes).contains("ix_doc_pet_criado").doesNotContain("residuo_antigo");
        assertThat(saida.getAll()).contains("índice residuo_antigo removido para recriar conforme o arquivo");
    }

    @SuppressWarnings("unchecked")
    private List<String> chaveDe(String nomeIndice) {
        Document indice = colecao.listIndexes().into(new ArrayList<>()).stream()
                .filter(i -> nomeIndice.equals(i.getString("name"))).findFirst().orElseThrow();
        return new ArrayList<>(((Document) indice.get("key")).keySet());
    }
}
