package br.com.clyvo.kura.tutor.laudo.infra;

import br.com.clyvo.kura.tutor.laudo.application.LaudoLeituraRepositorio;
import br.com.clyvo.kura.tutor.laudo.application.LaudoTutor;
import br.com.clyvo.kura.tutor.shared.mongo.MongodEmbarcado;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra um mongod real: documentos no formato EXATO do contrato laudos v1 (dono .NET), inclusive
 * os campos internos que o Java não pode devolver. Consequência declarada do contrato: o escopo
 * do tutor é o PET, então laudo de QUALQUER clínica para aquele idPet aparece (idClinica não filtra).
 */
@Tag("mongo-real")
class LaudoMongoLeituraRealTest {

    private static final long PET = 10L;
    private static MongoClient cliente;

    private MongoCollection<Document> colecao;
    private LaudoMongoLeitura repositorio;
    private MongoTemplate template;

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
        colecao = cliente.getDatabase(nome).getCollection("laudos");
        template = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, nome));
        repositorio = new LaudoMongoLeitura(template);
    }

    /** Documento completo do contrato v1 (todos os campos obrigatórios + os internos). */
    private static Document laudo(long idClinica, long idPet, String titulo, String dataExame, boolean ativo) {
        return new Document("idClinica", idClinica)
                .append("idPet", idPet)
                .append("idEventoClinico", 55L)
                .append("idUsuarioAutor", 9L)
                .append("tipo", "HEMOGRAMA")
                .append("titulo", titulo)
                .append("dataExame", Date.from(Instant.parse(dataExame)))
                .append("laboratorio", "Lab Vet")
                .append("observacao", "NOTA INTERNA DA CLINICA")
                .append("dados", new Document("hematocrito", 41.5))
                .append("arquivo", new Document("chave",
                                "clinica/" + idClinica + "/pet/" + idPet + "/laudo/0a1b2c3d4e5f40008000000000abcdef.pdf")
                        .append("contentType", "application/pdf")
                        .append("tamanhoBytes", 2048L)
                        .append("sha256", "a".repeat(64)))
                .append("stAtivo", ativo)
                .append("criadoEm", new Date())
                .append("atualizadoEm", new Date())
                .append("removidoEm", null);
    }

    @Test
    @DisplayName("devolve só stAtivo:true do pet pedido, dataExame decrescente; inclui outra clínica com o mesmo idPet")
    void filtraOrdenaEEscopoEhOPet() {
        colecao.insertOne(laudo(1, PET, "antigo", "2026-01-10T00:00:00Z", true));
        colecao.insertOne(laudo(1, PET, "recente", "2026-09-10T00:00:00Z", true));
        colecao.insertOne(laudo(2, PET, "outra-clinica", "2026-05-10T00:00:00Z", true));
        colecao.insertOne(laudo(1, PET, "removido", "2026-12-10T00:00:00Z", false));
        colecao.insertOne(laudo(1, 99L, "outro-pet", "2026-10-10T00:00:00Z", true));

        List<LaudoTutor> lista = repositorio.listarAtivos(PET);

        assertThat(lista).extracting(LaudoTutor::titulo).containsExactly("recente", "outra-clinica", "antigo");
    }

    @Test
    @DisplayName("projeção: traz os campos de exibição e a chave; nunca observacao, dados, autoria nem clínica")
    void projecaoSemCamposInternos() {
        colecao.insertOne(laudo(1, PET, "hemograma", "2026-09-10T00:00:00Z", true));

        LaudoTutor l = repositorio.listarAtivos(PET).get(0);

        assertThat(l.tipo()).isEqualTo("HEMOGRAMA");
        assertThat(l.laboratorio()).isEqualTo("Lab Vet");
        assertThat(l.dataExame()).isEqualTo(Instant.parse("2026-09-10T00:00:00Z"));
        assertThat(l.contentType()).isEqualTo("application/pdf");
        assertThat(l.tamanhoBytes()).isEqualTo(2048L);
        assertThat(l.chaveArquivo()).endsWith(".pdf");
        // O record não tem campo para os internos; a prova de que nem saem do banco é a consulta REAL do
        // repositório executada crua: o documento devolvido só tem as chaves da lista positiva.
        Document cru = template.find(LaudoMongoLeitura.consulta(PET), Document.class, "laudos").get(0);
        assertThat(cru.keySet()).containsExactlyInAnyOrder("_id", "tipo", "titulo", "dataExame", "laboratorio", "arquivo");
        assertThat(cru.get("arquivo", Document.class).keySet())
                .containsExactlyInAnyOrder("chave", "contentType", "tamanhoBytes");
    }

    @Test
    @DisplayName("teto de leitura: no máximo 100 laudos, os mais recentes")
    void tetoDeLeitura() {
        for (int i = 0; i < LaudoLeituraRepositorio.TETO_LEITURA + 5; i++) {
            colecao.insertOne(laudo(1, PET, "l" + i, Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i * 60L).toString(), true));
        }

        List<LaudoTutor> lista = repositorio.listarAtivos(PET);

        assertThat(lista).hasSize(LaudoLeituraRepositorio.TETO_LEITURA);
        assertThat(lista.get(0).titulo()).isEqualTo("l104");
    }

    @Test
    @DisplayName("o filtro do tutor é atendido pelo índice ix_laudo_pet_ativo_data (IXSCAN, sem SORT em memória)")
    void usaOIndiceDoContrato() {
        colecao.createIndex(new Document("idPet", 1).append("stAtivo", 1).append("dataExame", -1),
                new com.mongodb.client.model.IndexOptions().name("ix_laudo_pet_ativo_data"));
        colecao.insertOne(laudo(1, PET, "x", "2026-09-10T00:00:00Z", true));

        // A consulta REAL do repositório (filtro, sort, limite, projeção), não uma reescrita no teste.
        Query q = LaudoMongoLeitura.consulta(PET);
        Document plano = colecao.find(q.getQueryObject()).projection(q.getFieldsObject())
                .sort(q.getSortObject()).limit(q.getLimit())
                .explain();
        String texto = plano.toJson();

        assertThat(texto).contains("IXSCAN").contains("ix_laudo_pet_ativo_data").doesNotContain("\"stage\": \"SORT\"");
    }

    @Test
    @DisplayName("documento malformado (sem dataExame / sem arquivo / tipo errado) é pulado com 1 WARN só com o _id; os demais voltam")
    void malformadoEPuladoComWarnSoComId() {
        colecao.insertOne(laudo(1, PET, "bom", "2026-09-10T00:00:00Z", true));
        Document semData = laudo(1, PET, "SEGREDO-SEM-DATA", "2026-09-11T00:00:00Z", true);
        semData.remove("dataExame");
        Document semArquivo = laudo(1, PET, "SEGREDO-SEM-ARQUIVO", "2026-09-12T00:00:00Z", true);
        semArquivo.remove("arquivo");
        Document tipoErrado = laudo(1, PET, "SEGREDO-TIPO-ERRADO", "2026-09-13T00:00:00Z", true);
        tipoErrado.put("dataExame", "2026-09-13");
        colecao.insertMany(List.of(semData, semArquivo, tipoErrado));

        Logger logger = (Logger) LoggerFactory.getLogger(LaudoMongoLeitura.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        List<LaudoTutor> lista;
        try {
            lista = repositorio.listarAtivos(PET);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(lista).extracting(LaudoTutor::titulo).containsExactly("bom");
        assertThat(appender.list).hasSize(3).allSatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).matches(".*_id=[0-9a-f]{24}\)$")
                    .doesNotContain("SEGREDO").doesNotContain("clinica/").doesNotContain("HEMOGRAMA");
        });
    }
}
