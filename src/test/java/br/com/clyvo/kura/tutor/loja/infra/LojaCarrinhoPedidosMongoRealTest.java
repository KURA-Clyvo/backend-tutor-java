package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.loja.application.CarrinhoSalvo;
import br.com.clyvo.kura.tutor.loja.application.CarrinhoService;
import br.com.clyvo.kura.tutor.loja.application.ItemPedido;
import br.com.clyvo.kura.tutor.loja.application.LimiteCarrinhoException;
import br.com.clyvo.kura.tutor.loja.application.LojaCarrinho;
import br.com.clyvo.kura.tutor.loja.application.LojaException;
import br.com.clyvo.kura.tutor.loja.application.NovoPedido;
import br.com.clyvo.kura.tutor.loja.application.PedidoLoja;
import br.com.clyvo.kura.tutor.loja.application.PedidoLojaService;
import br.com.clyvo.kura.tutor.loja.application.ResultadoPedido;
import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.application.StatusPedido;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.CarrinhoResponse;
import br.com.clyvo.kura.tutor.shared.mongo.MongodEmbarcado;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Carrinho e pedidos da Loja contra um mongod 7.0.x REAL embarcado: os schemas e índices dos
 * arquivos versionados (pelo runner de produção), os repositórios concretos e os services, com
 * dois tutores. O banco do teste NÃO tem o índice TTL nos casos de carrinho vencido (o monitor
 * do mongod, a cada 60 s, apagaria o documento forjado e o teste ficaria verde por sorte).
 */
@Tag("mongo-real")
class LojaCarrinhoPedidosMongoRealTest {

    private static final String SHAMPOO = "shampoo-neutro-500ml";
    private static final String ESCOVA = "escova-cerdas-macias";
    private static final long TUTOR_A = 1L;
    private static final long TUTOR_B = 2L;

    private static MongoClient cliente;

    private MongoDatabase database;
    private MongoTemplate template;
    private CarrinhoMongoRepositorio carrinhos;
    private PedidoLojaMongoRepositorio pedidos;
    private CarrinhoService carrinhoService;
    private PedidoLojaService pedidoService;
    private MongoCollection<Document> colCarrinho;
    private MongoCollection<Document> colPedidos;
    private MongoCollection<Document> colCatalogo;

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
        String nome = "kura_loja_" + UUID.randomUUID().toString().replace("-", "");
        database = cliente.getDatabase(nome);
        template = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, nome));
        new SchemaCatalogoAplicador(database).aplicar();
        new SchemaCarrinhoAplicador(database).aplicar();
        new SchemaPedidosAplicador(database).aplicar();
        colCarrinho = database.getCollection("carrinho");
        colPedidos = database.getCollection("pedidos_loja");
        colCatalogo = database.getCollection("catalogo_loja");
        carrinhos = new CarrinhoMongoRepositorio(template);
        pedidos = new PedidoLojaMongoRepositorio(template);
        CatalogoMongoRepositorio catalogo = new CatalogoMongoRepositorio(template);
        carrinhoService = new CarrinhoService(carrinhos, catalogo, Clock.systemUTC());
        pedidoService = new PedidoLojaService(pedidos, carrinhos, catalogo, Clock.systemUTC());
        produto(SHAMPOO, 2990, true);
        produto(ESCOVA, 2490, true);
    }

    private void produto(String slug, long preco, boolean ativo) {
        colCatalogo.insertOne(new Document("slug", slug).append("nome", "Nome " + slug).append("categoria", "HIGIENE")
                .append("preco", preco).append("moeda", "BRL").append("ativo", ativo).append("ordem", 10L)
                .append("exemplo", true)
                .append("dados", new Document("tipoHigiene", "SHAMPOO").append("especieAlvo", "AMBOS"))
                .append("criadoEm", new Date()).append("atualizadoEm", new Date()));
    }

    /** O índice TTL sai do banco do teste: o monitor do mongod apagaria o carrinho forjado e o teste passaria por sorte. */
    private void semTtl() {
        colCarrinho.dropIndex("ttl_carrinho_atualizadoEm");
    }

    private void forjarAtualizadoEm(long idTutor, Instant quando) {
        colCarrinho.updateOne(Filters.eq("_id", idTutor), Updates.set("atualizadoEm", Date.from(quando)));
    }

    private static String chave() {
        return UUID.randomUUID().toString();
    }

    private int quantidadeNoBanco(long idTutor, String slug) {
        Document doc = colCarrinho.find(Filters.eq("_id", idTutor)).first();
        return ((Number) doc.get("itens", Document.class).get(slug)).intValue();
    }

    private void recusa(MongoCollection<Document> colecao, Document doc) {
        assertThatThrownBy(() -> colecao.insertOne(doc))
                .isInstanceOfSatisfying(MongoWriteException.class, e -> assertThat(e.getCode()).isEqualTo(121));
    }

    // ---------------------------------------------------------------- 1. runner, schema e índices

    @Test
    @DisplayName("runner: cria as 2 coleções com validator strict/error e os 3 índices; TTL = 7200 = VALIDADE; reaplicar é idempotente")
    void runnerCriaColecoesEIndices() {
        String nome = "kura_runner_" + UUID.randomUUID().toString().replace("-", "");
        MongoDatabase limpo = cliente.getDatabase(nome);
        LojaMongoInicializador runner = new LojaMongoInicializador(new SimpleMongoClientDatabaseFactory(cliente, nome));

        runner.run(null);
        runner.run(null);

        for (String colecao : List.of("carrinho", "pedidos_loja")) {
            Document opcoes = limpo.listCollections().into(new ArrayList<>()).stream()
                    .filter(c -> colecao.equals(c.getString("name"))).findFirst().orElseThrow()
                    .get("options", Document.class);
            assertThat(opcoes.getString("validationLevel")).isEqualTo("strict");
            assertThat(opcoes.getString("validationAction")).isEqualTo("error");
            assertThat(opcoes.get("validator", Document.class)).isNotNull();
        }
        List<Document> idxCarrinho = limpo.getCollection("carrinho").listIndexes().into(new ArrayList<>());
        Document ttl = idxCarrinho.stream().filter(i -> "ttl_carrinho_atualizadoEm".equals(i.getString("name")))
                .findFirst().orElseThrow();
        assertThat(((Number) ttl.get("expireAfterSeconds")).longValue()).isEqualTo(7200L);
        assertThat(ttl.get("expireAfterSeconds")).isEqualTo((int) LojaCarrinho.VALIDADE.toSeconds());
        List<Document> idxPedidos = limpo.getCollection("pedidos_loja").listIndexes().into(new ArrayList<>());
        assertThat(idxPedidos).extracting(i -> i.getString("name"))
                .contains("uk_pedido_tutor_chave", "ix_pedido_tutor_criado");
        Document unico = idxPedidos.stream().filter(i -> "uk_pedido_tutor_chave".equals(i.getString("name")))
                .findFirst().orElseThrow();
        assertThat(unico.getBoolean("unique")).isTrue();
        assertThat(((Document) unico.get("key")).keySet()).containsExactly("idTutor", "chaveIdempotencia");
    }

    @Test
    @DisplayName("E61: TTL do banco divergente do arquivo (3600 x 7200, mesmo nome, código 85) é removido e recriado COM o expireAfterSeconds do arquivo")
    void e61RecriaComTtl() {
        colCarrinho.dropIndex("ttl_carrinho_atualizadoEm");
        colCarrinho.createIndex(BsonDocument.parse("{\"atualizadoEm\": 1}"),
                new com.mongodb.client.model.IndexOptions().name("ttl_carrinho_atualizadoEm")
                        .expireAfter(3600L, java.util.concurrent.TimeUnit.SECONDS));

        new SchemaCarrinhoAplicador(database).aplicar();

        Document ttl = colCarrinho.listIndexes().into(new ArrayList<>()).stream()
                .filter(i -> "ttl_carrinho_atualizadoEm".equals(i.getString("name"))).findFirst().orElseThrow();
        assertThat(ttl.get("expireAfterSeconds")).isEqualTo(7200);
    }

    // ---------------------------------------------------------------- 2. o que o validator recusa

    @Test
    @DisplayName("validator do carrinho: aceita o válido; recusa quantidade 0 e 100, slug com maiúscula, '.', campo extra e o 51º produto")
    void validatorDoCarrinho() {
        colCarrinho.insertOne(new Document("_id", 7L).append("itens", new Document("ok-1", 99))
                .append("criadoEm", new Date()).append("atualizadoEm", new Date()));
        assertThat(colCarrinho.countDocuments()).isEqualTo(1);

        recusa(colCarrinho, doc(8L, new Document("ok-1", 0)));
        recusa(colCarrinho, doc(8L, new Document("ok-1", 100)));
        recusa(colCarrinho, doc(8L, new Document("Shampoo", 1)));
        recusa(colCarrinho, doc(8L, new Document("a.b", 1)));
        recusa(colCarrinho, doc(8L, new Document("a$b", 1)));
        recusa(colCarrinho, doc(8L, new Document("ok-1", 1)).append("extra", 1));
        Document cinquentaEUm = new Document();
        for (int i = 0; i < 51; i++) {
            cinquentaEUm.append("produto-" + i, 1);
        }
        recusa(colCarrinho, doc(8L, cinquentaEUm));
    }

    private static Document doc(long id, Document itens) {
        return new Document("_id", id).append("itens", itens).append("criadoEm", new Date())
                .append("atualizadoEm", new Date());
    }

    private Document pedidoValido() {
        return new Document("idTutor", TUTOR_A).append("chaveIdempotencia", chave())
                .append("status", "AGUARDANDO_PARCEIRO")
                .append("itens", List.of(new Document("slug", SHAMPOO).append("nome", "n").append("categoria", "HIGIENE")
                        .append("precoUnitario", 2990L).append("quantidade", 1).append("exemplo", true)))
                .append("total", 2990L).append("moeda", "BRL").append("carrinhoVersao", new Date())
                .append("criadoEm", new Date()).append("atualizadoEm", new Date());
    }

    @Test
    @DisplayName("validator do pedido: aceita o válido; recusa status PAGO, sem itens, chave em maiúsculas, campo de pagamento e idTutor int32")
    void validatorDoPedido() {
        colPedidos.insertOne(pedidoValido());
        assertThat(colPedidos.countDocuments()).isEqualTo(1);

        recusa(colPedidos, pedidoValido().append("status", "PAGO"));
        recusa(colPedidos, pedidoValido().append("itens", List.of()));
        recusa(colPedidos, pedidoValido().append("chaveIdempotencia", UUID.randomUUID().toString().toUpperCase()));
        recusa(colPedidos, pedidoValido().append("formaDePagamento", "PIX"));
        Document intId = pedidoValido();
        intId.put("idTutor", 1);
        recusa(colPedidos, intId);
    }

    // ---------------------------------------------------------------- 3. $inc, limites e a exceção real do 121

    @Test
    @DisplayName("10 threads somando +1 no mesmo produto dão 10 (o $inc é atômico, o upsert concorrente não dá DuplicateKey)")
    void incConcorrenteEAtomico() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                carrinhos.somar(TUTOR_A, SHAMPOO, 1, Instant.now());
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> f : futuros) {
            f.get();
        }
        pool.shutdown();

        assertThat(quantidadeNoBanco(TUTOR_A, SHAMPOO)).isEqualTo(10);
    }

    @Test
    @DisplayName("a exceção REAL do 121 num upsert com Document: DataIntegrityViolationException <- MongoWriteException(121), e NÃO DuplicateKeyException")
    void excecaoRealDoCodigo121() {
        carrinhos.somar(TUTOR_A, SHAMPOO, 98, Instant.now());

        Throwable erro = catchThrowable(() -> template.upsert(
                new Query(Criteria.where("_id").is(TUTOR_A)), new Update().inc("itens." + SHAMPOO, 5), "carrinho"));

        assertThat(erro).isInstanceOf(DataIntegrityViolationException.class).isNotInstanceOf(DuplicateKeyException.class);
        assertThat(erro.getCause()).isInstanceOfSatisfying(MongoWriteException.class,
                e -> assertThat(e.getCode()).isEqualTo(121));
        assertThat(quantidadeNoBanco(TUTOR_A, SHAMPOO)).isEqualTo(98);
    }

    @Test
    @DisplayName("98 + 5 vira LimiteCarrinhoException (422 LIMITE_CARRINHO) e o valor continua 98")
    void somaAcimaDe99() {
        carrinhos.somar(TUTOR_A, SHAMPOO, 98, Instant.now());

        assertThatThrownBy(() -> carrinhos.somar(TUTOR_A, SHAMPOO, 5, Instant.now()))
                .isInstanceOf(LimiteCarrinhoException.class)
                .satisfies(e -> {
                    assertThat(((LojaException) e).status()).isEqualTo(422);
                    assertThat(((LojaException) e).codigo()).isEqualTo("LIMITE_CARRINHO");
                    assertThat(e.getMessage()).doesNotContain(SHAMPOO).doesNotContain("1");
                });
        assertThat(quantidadeNoBanco(TUTOR_A, SHAMPOO)).isEqualTo(98);
    }

    @Test
    @DisplayName("os 3 casos de 422: soma acima de 99 num slug novo (via PUT não, via POST 100), estouro em slug EXISTENTE com 50 itens, e o 51º produto: todos LIMITE_CARRINHO")
    void tresCasosDeLimite() {
        for (int i = 0; i < 49; i++) {
            carrinhos.somar(TUTOR_A, "produto-" + i, 1, Instant.now());
        }
        carrinhos.somar(TUTOR_A, SHAMPOO, 98, Instant.now());
        assertThat(colCarrinho.find(Filters.eq("_id", TUTOR_A)).first().get("itens", Document.class)).hasSize(50);

        assertThatThrownBy(() -> carrinhos.somar(TUTOR_A, SHAMPOO, 5, Instant.now()))
                .isInstanceOf(LimiteCarrinhoException.class);
        assertThatThrownBy(() -> carrinhos.somar(TUTOR_A, "produto-novo", 1, Instant.now()))
                .isInstanceOf(LimiteCarrinhoException.class);
        assertThatThrownBy(() -> carrinhos.definir(TUTOR_A, "produto-novo", 1, Instant.now()))
                .isInstanceOf(LimiteCarrinhoException.class);
        assertThatThrownBy(() -> carrinhos.somar(TUTOR_B, SHAMPOO, 100, Instant.now()))
                .isInstanceOf(LimiteCarrinhoException.class);
        assertThat(colCarrinho.find(Filters.eq("_id", TUTOR_A)).first().get("itens", Document.class)).hasSize(50);
    }

    @Test
    @DisplayName("PUT define o valor absoluto (idempotente) e remover item ausente não é erro")
    void definirERemover() {
        carrinhoService.definir(TUTOR_A, SHAMPOO, 3);
        carrinhoService.definir(TUTOR_A, SHAMPOO, 3);
        assertThat(quantidadeNoBanco(TUTOR_A, SHAMPOO)).isEqualTo(3);

        CarrinhoResponse r = carrinhoService.remover(TUTOR_A, "nao-estava-no-carrinho");
        assertThat(r.itens()).hasSize(1);
        r = carrinhoService.remover(TUTOR_A, SHAMPOO);
        assertThat(r.itens()).isEmpty();
        assertThat(r.expiraEm()).isNull();
    }

    @Test
    @DisplayName("produto inexistente ou inativo: 404 PRODUTO_NAO_ENCONTRADO ao adicionar e ao definir; remover não consulta o catálogo")
    void produtoInativo() {
        produto("produto-inativo", 1000, false);

        assertThatThrownBy(() -> carrinhoService.adicionar(TUTOR_A, "nao-existe", 1))
                .isInstanceOfSatisfying(LojaException.class, e -> assertThat(e.codigo()).isEqualTo("PRODUTO_NAO_ENCONTRADO"));
        assertThatThrownBy(() -> carrinhoService.adicionar(TUTOR_A, "produto-inativo", 1))
                .isInstanceOf(LojaException.class);
        assertThatThrownBy(() -> carrinhoService.definir(TUTOR_A, "produto-inativo", 1))
                .isInstanceOf(LojaException.class);
        assertThat(colCarrinho.countDocuments()).isZero();
        carrinhoService.remover(TUTOR_A, "produto-inativo");
    }

    @Test
    @DisplayName("resposta do carrinho: itens por slug, total só dos disponíveis, quantidadeProdutos (HLEN) e quantidadeTotal, expiraEm = +2h")
    void respostaDoCarrinho() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 2);
        carrinhoService.adicionar(TUTOR_A, ESCOVA, 1);
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);

        CarrinhoResponse r = carrinhoService.ver(TUTOR_A);

        assertThat(r.itens()).extracting(i -> i.slug()).containsExactly(ESCOVA, SHAMPOO);
        assertThat(r.total()).isEqualTo(3 * 2990L + 2490L);
        assertThat(r.quantidadeProdutos()).isEqualTo(2);
        assertThat(r.quantidadeTotal()).isEqualTo(4);
        Instant atualizado = colCarrinho.find(Filters.eq("_id", TUTOR_A)).first().getDate("atualizadoEm").toInstant();
        assertThat(r.expiraEm()).isEqualTo(atualizado.plus(Duration.ofHours(2)));
        assertThat(r.aviso()).startsWith("Pré-lançamento");
    }

    // ---------------------------------------------------------------- 4. carrinho vencido (banco sem TTL)

    @Test
    @DisplayName("vencido (forjado há 3 h, sem TTL): a leitura dá vazio, adicionar NÃO ressuscita os itens antigos e pedir dá 422 CARRINHO_VAZIO")
    void carrinhoVencido() {
        semTtl();
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 5);
        forjarAtualizadoEm(TUTOR_A, Instant.now().minus(Duration.ofHours(3)));

        assertThat(carrinhoService.ver(TUTOR_A).itens()).isEmpty();
        assertThatThrownBy(() -> pedidoService.criar(TUTOR_A, chave()))
                .isInstanceOfSatisfying(LojaException.class, e -> assertThat(e.codigo()).isEqualTo("CARRINHO_VAZIO"));

        CarrinhoResponse depois = carrinhoService.adicionar(TUTOR_A, ESCOVA, 1);

        assertThat(depois.itens()).extracting(i -> i.slug()).containsExactly(ESCOVA);
        assertThat(colCarrinho.find(Filters.eq("_id", TUTOR_A)).first().get("itens", Document.class).keySet())
                .containsExactly(ESCOVA);
    }

    @Test
    @DisplayName("a leitura de vencido usa o limite de 2 h: carrinho de 1h59 aparece, de 2h01 não (sem TTL)")
    void limiteDaValidade() {
        semTtl();
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);

        forjarAtualizadoEm(TUTOR_A, Instant.now().minus(Duration.ofMinutes(119)));
        assertThat(carrinhoService.ver(TUTOR_A).itens()).hasSize(1);
        forjarAtualizadoEm(TUTOR_A, Instant.now().minus(Duration.ofMinutes(121)));
        assertThat(carrinhoService.ver(TUTOR_A).itens()).isEmpty();
    }

    // ---------------------------------------------------------------- 5. idempotência

    @Test
    @DisplayName("mesma chave duas vezes: 1 documento, 201 e depois 200 com o mesmo id; o carrinho é apagado")
    void mesmaChaveDuasVezes() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 2);
        String k = chave();

        ResultadoPedido primeiro = pedidoService.criar(TUTOR_A, k);
        ResultadoPedido segundo = pedidoService.criar(TUTOR_A, k.toUpperCase());

        assertThat(primeiro.criado()).isTrue();
        assertThat(segundo.criado()).isFalse();
        assertThat(segundo.pedido().id()).isEqualTo(primeiro.pedido().id());
        assertThat(colPedidos.countDocuments()).isEqualTo(1);
        assertThat(primeiro.pedido().status()).isEqualTo(StatusPedido.AGUARDANDO_PARCEIRO);
        assertThat(primeiro.pedido().total()).isEqualTo(2 * 2990L);
        assertThat(colCarrinho.countDocuments()).isZero();
    }

    @Test
    @DisplayName("2 threads com a mesma chave geram 1 documento e nenhuma falha")
    void duasThreadsMesmaChave() throws Exception {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
        String k = chave();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<ResultadoPedido>> futuros = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                return pedidoService.criar(TUTOR_A, k);
            }));
        }
        largada.countDown();
        String id0 = futuros.get(0).get().pedido().id();
        String id1 = futuros.get(1).get().pedido().id();
        pool.shutdown();

        assertThat(id0).isEqualTo(id1);
        assertThat(colPedidos.countDocuments()).isEqualTo(1);
    }

    @Test
    @DisplayName("passo 4 determinístico (G2-I7): o passo 1 não enxerga o pedido, o insert colide (DuplicateKey) e o service devolve o existente (200) e apaga o carrinho")
    void passoQuatroDeterministico() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 2);
        CarrinhoSalvo carrinho = carrinhos.buscarValido(TUTOR_A, Instant.now().minus(Duration.ofHours(2))).orElseThrow();
        String k = chave();
        PedidoLoja existente = pedidos.inserir(TUTOR_A, new NovoPedido(k,
                List.of(new ItemPedido(SHAMPOO, "n", CategoriaLoja.HIGIENE, 2990, 2, true)), 5980,
                carrinho.atualizadoEm(), Instant.now()));
        PedidoLojaMongoRepositorio espiao = spy(pedidos);
        doReturn(Optional.empty()).doCallRealMethod().when(espiao).buscarPorChave(anyLong(), anyString());
        PedidoLojaService service = new PedidoLojaService(espiao, carrinhos, new CatalogoMongoRepositorio(template),
                Clock.systemUTC());

        ResultadoPedido resultado = service.criar(TUTOR_A, k);

        assertThat(resultado.criado()).isFalse();
        assertThat(resultado.pedido().id()).isEqualTo(existente.id());
        assertThat(colPedidos.countDocuments()).isEqualTo(1);
        assertThat(colCarrinho.countDocuments()).isZero();
    }

    @Test
    @DisplayName("corrida da mesma chave (I-1 da G2 da 09): o perdedor passa pelo passo 1 antes do insert e chega ao passo 2 depois do passo 5 do vencedor: 200 com o mesmo pedido, não 422")
    void perdedorChegaAoPasso2DepoisDoPasso5DoVencedor() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
        String k = chave();
        ResultadoPedido vencedor = pedidoService.criar(TUTOR_A, k);
        assertThat(colCarrinho.countDocuments()).isZero();
        PedidoLojaMongoRepositorio espiao = spy(pedidos);
        doReturn(Optional.empty()).doCallRealMethod().when(espiao).buscarPorChave(anyLong(), anyString());
        PedidoLojaService perdedor = new PedidoLojaService(espiao, carrinhos, new CatalogoMongoRepositorio(template),
                Clock.systemUTC());

        ResultadoPedido r = perdedor.criar(TUTOR_A, k);

        assertThat(r.criado()).isFalse();
        assertThat(r.pedido().id()).isEqualTo(vencedor.pedido().id());
        assertThat(colPedidos.countDocuments()).isEqualTo(1);
        assertThat(colCarrinho.countDocuments()).isZero();
    }

    @Test
    @DisplayName("falha entre os passos 3 e 5: pedido gravado e carrinho intacto; o retry com a mesma chave apaga o carrinho")
    void retryApagaOCarrinhoConsumido() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 2);
        CarrinhoSalvo carrinho = carrinhos.buscarValido(TUTOR_A, Instant.now().minus(Duration.ofHours(2))).orElseThrow();
        String k = chave();
        pedidos.inserir(TUTOR_A, new NovoPedido(k,
                List.of(new ItemPedido(SHAMPOO, "n", CategoriaLoja.HIGIENE, 2990, 2, true)), 5980,
                carrinho.atualizadoEm(), Instant.now()));
        assertThat(colCarrinho.countDocuments()).isEqualTo(1);

        ResultadoPedido retry = pedidoService.criar(TUTOR_A, k);

        assertThat(retry.criado()).isFalse();
        assertThat(colCarrinho.countDocuments()).isZero();
        assertThat(colPedidos.countDocuments()).isEqualTo(1);
    }

    @Test
    @DisplayName("carrinho alterado DEPOIS do pedido: o retry com a mesma chave NÃO apaga o carrinho novo")
    void retryNaoApagaCarrinhoAlterado() throws InterruptedException {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 2);
        CarrinhoSalvo carrinho = carrinhos.buscarValido(TUTOR_A, Instant.now().minus(Duration.ofHours(2))).orElseThrow();
        String k = chave();
        pedidos.inserir(TUTOR_A, new NovoPedido(k,
                List.of(new ItemPedido(SHAMPOO, "n", CategoriaLoja.HIGIENE, 2990, 2, true)), 5980,
                carrinho.atualizadoEm(), Instant.now()));
        Thread.sleep(20);
        carrinhoService.adicionar(TUTOR_A, ESCOVA, 1);

        pedidoService.criar(TUTOR_A, k);

        assertThat(colCarrinho.countDocuments()).isEqualTo(1);
        assertThat(quantidadeNoBanco(TUTOR_A, ESCOVA)).isEqualTo(1);
        assertThat(quantidadeNoBanco(TUTOR_A, SHAMPOO)).isEqualTo(2);
    }

    @Test
    @DisplayName("a mesma chave em tutores diferentes são pedidos diferentes (a idempotência é por tutor)")
    void chaveEPorTutor() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
        carrinhoService.adicionar(TUTOR_B, SHAMPOO, 1);
        String k = chave();

        ResultadoPedido a = pedidoService.criar(TUTOR_A, k);
        ResultadoPedido b = pedidoService.criar(TUTOR_B, k);

        assertThat(a.criado()).isTrue();
        assertThat(b.criado()).isTrue();
        assertThat(a.pedido().id()).isNotEqualTo(b.pedido().id());
    }

    @Test
    @DisplayName("Idempotency-Key que não é UUID: 400 IDEMPOTENCY_KEY_INVALIDA e nada é gravado")
    void chaveInvalida() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);

        for (String ruim : new String[] {"abc", "", null, UUID.randomUUID() + "\n", UUID.randomUUID() + "x"}) {
            assertThatThrownBy(() -> pedidoService.criar(TUTOR_A, ruim))
                    .isInstanceOfSatisfying(LojaException.class, e -> {
                        assertThat(e.status()).isEqualTo(400);
                        assertThat(e.codigo()).isEqualTo("IDEMPOTENCY_KEY_INVALIDA");
                    });
        }
        assertThat(colPedidos.countDocuments()).isZero();
        assertThat(colCarrinho.countDocuments()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- 6. snapshot e item indisponível

    @Test
    @DisplayName("snapshot: mudar o preço e o nome no catálogo depois do pedido não muda o pedido")
    void snapshotDoPedido() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 2);
        PedidoLoja criado = pedidoService.criar(TUTOR_A, chave()).pedido();

        colCatalogo.updateOne(Filters.eq("slug", SHAMPOO), Updates.combine(Updates.set("preco", 9999L), Updates.set("nome", "Outro nome")));

        PedidoLoja lido = pedidoService.detalhar(TUTOR_A, criado.id());
        assertThat(lido.itens().get(0).precoUnitario()).isEqualTo(2990L);
        assertThat(lido.itens().get(0).nome()).isEqualTo("Nome " + SHAMPOO);
        assertThat(lido.total()).isEqualTo(5980L);
    }

    @Test
    @DisplayName("item desativado no carrinho: disponivel=false, fora do total; o pedido dá 422 ITEM_INDISPONIVEL com o slug em detalhes e nada é gravado")
    void itemIndisponivel() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
        carrinhoService.adicionar(TUTOR_A, ESCOVA, 1);
        colCatalogo.updateOne(Filters.eq("slug", ESCOVA), Updates.set("ativo", false));

        CarrinhoResponse r = carrinhoService.ver(TUTOR_A);

        assertThat(r.total()).isEqualTo(2990L);
        var escova = r.itens().stream().filter(i -> ESCOVA.equals(i.slug())).findFirst().orElseThrow();
        assertThat(escova.disponivel()).isFalse();
        assertThat(escova.precoUnitario()).isNull();
        assertThat(escova.subtotal()).isNull();
        assertThat(escova.nome()).isEqualTo(ESCOVA);
        assertThatThrownBy(() -> pedidoService.criar(TUTOR_A, chave()))
                .isInstanceOfSatisfying(LojaException.class, e -> {
                    assertThat(e.codigo()).isEqualTo("ITEM_INDISPONIVEL");
                    assertThat(e.detalhes()).containsExactly(ESCOVA);
                });
        assertThat(colPedidos.countDocuments()).isZero();
        assertThat(colCarrinho.countDocuments()).isEqualTo(1);
    }

    @Test
    @DisplayName("carrinho vazio: 422 CARRINHO_VAZIO")
    void pedidoComCarrinhoVazio() {
        assertThatThrownBy(() -> pedidoService.criar(TUTOR_A, chave()))
                .isInstanceOfSatisfying(LojaException.class, e -> assertThat(e.codigo()).isEqualTo("CARRINHO_VAZIO"));
    }

    // ---------------------------------------------------------------- 7. cancelar e listar

    @Test
    @DisplayName("cancelar: 200 com canceladoEm; cancelar de novo é idempotente e NÃO reescreve o canceladoEm (relógio avança 1 h entre as chamadas)")
    void cancelarEIdempotente() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
        PedidoLoja criado = pedidoService.criar(TUTOR_A, chave()).pedido();
        assertThat(criado.canceladoEm()).isNull();
        Instant base = Instant.now();
        int[] chamadas = {0};
        Clock relogioQueAvanca = new Clock() {
            @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return base.plus(Duration.ofHours(chamadas[0]++)); }
        };
        PedidoLojaService service = new PedidoLojaService(pedidos, carrinhos, new CatalogoMongoRepositorio(template),
                relogioQueAvanca);

        PedidoLoja cancelado = service.cancelar(TUTOR_A, criado.id());
        PedidoLoja denovo = service.cancelar(TUTOR_A, criado.id());

        assertThat(cancelado.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(cancelado.canceladoEm()).isNotNull();
        assertThat(denovo.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(denovo.canceladoEm()).isEqualTo(cancelado.canceladoEm());
        assertThat(chamadas[0]).isEqualTo(2);
        assertThat(colPedidos.countDocuments()).isEqualTo(1);
    }

    @Test
    @DisplayName("listar: do mais novo ao mais antigo, com Slice (hasNext) e page")
    void listarPaginado() throws InterruptedException {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
            ids.add(pedidoService.criar(TUTOR_A, chave()).pedido().id());
            Thread.sleep(5);
        }

        var primeira = pedidoService.listar(TUTOR_A, 0, 2);
        var segunda = pedidoService.listar(TUTOR_A, 1, 2);

        assertThat(primeira.pedidos()).extracting(PedidoLoja::id).containsExactly(ids.get(2), ids.get(1));
        assertThat(primeira.temProxima()).isTrue();
        assertThat(segunda.pedidos()).extracting(PedidoLoja::id).containsExactly(ids.get(0));
        assertThat(segunda.temProxima()).isFalse();
    }

    // ---------------------------------------------------------------- 8. dois tutores

    @Test
    @DisplayName("dois tutores: o tutor B vê carrinho vazio, recebe 404 no detalhe e no cancelamento do pedido de A, e o status de A relido do banco não muda")
    void doisTutores() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
        String idA = pedidoService.criar(TUTOR_A, chave()).pedido().id();
        carrinhoService.adicionar(TUTOR_A, ESCOVA, 1);

        assertThat(carrinhoService.ver(TUTOR_B).itens()).isEmpty();
        assertThatThrownBy(() -> pedidoService.detalhar(TUTOR_B, idA))
                .isInstanceOfSatisfying(LojaException.class, e -> assertThat(e.status()).isEqualTo(404));
        assertThatThrownBy(() -> pedidoService.cancelar(TUTOR_B, idA))
                .isInstanceOfSatisfying(LojaException.class, e -> {
                    assertThat(e.status()).isEqualTo(404);
                    assertThat(e.codigo()).isEqualTo("PEDIDO_NAO_ENCONTRADO");
                });

        assertThat(colPedidos.find().first().getString("status")).isEqualTo("AGUARDANDO_PARCEIRO");
        assertThat(colPedidos.find().first().get("canceladoEm")).isNull();
        assertThat(pedidoService.listar(TUTOR_B, 0, 20).pedidos()).isEmpty();
        assertThat(pedidoService.listar(TUTOR_A, 0, 20).pedidos()).hasSize(1);
        carrinhoService.esvaziar(TUTOR_B);
        assertThat(carrinhoService.ver(TUTOR_A).itens()).hasSize(1);
        assertThat(pedidoService.detalhar(TUTOR_A, idA).status()).isEqualTo(StatusPedido.AGUARDANDO_PARCEIRO);
    }

    @Test
    @DisplayName("id malformado e id de outro tutor dão a MESMA resposta 404 (não revela que o pedido existe)")
    void idMalformadoIgualAPedidoAlheio() {
        carrinhoService.adicionar(TUTOR_A, SHAMPOO, 1);
        String idA = pedidoService.criar(TUTOR_A, chave()).pedido().id();

        LojaException alheio = (LojaException) catchThrowable(() -> pedidoService.detalhar(TUTOR_B, idA));
        LojaException malformado = (LojaException) catchThrowable(() -> pedidoService.detalhar(TUTOR_B, "nao-e-objectid"));

        assertThat(malformado.status()).isEqualTo(alheio.status());
        assertThat(malformado.codigo()).isEqualTo(alheio.codigo());
        assertThat(malformado.getMessage()).isEqualTo(alheio.getMessage());
    }
}
