// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.infra;

import br.com.clyvo.kura.tutor.loja.application.CategoriaLoja;
import br.com.clyvo.kura.tutor.loja.application.FatiaCatalogo;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class CatalogoMongoRepositorioTest {

    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final CatalogoMongoRepositorio repositorio = new CatalogoMongoRepositorio(mongo);

    private static Document doc(int n) {
        return new Document("slug", "item-" + n).append("nome", "Item " + n).append("categoria", "HIGIENE")
                .append("preco", 100L * n).append("moeda", "BRL").append("exemplo", true)
                .append("dados", new Document("tipoHigiene", "ESCOVA"));
    }

    @Test
    @DisplayName("consulta: ativo=true, ordem+slug, skip=page*size, limit=size+1")
    void montaAConsultaDosIndices() {
        when(mongo.find(org.mockito.ArgumentMatchers.any(Query.class), eq(Document.class), eq("catalogo_loja")))
                .thenReturn(List.of());

        repositorio.listarAtivos(null, 3, 20);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Document.class), eq("catalogo_loja"));
        assertThat(query.getValue().getQueryObject().toJson()).isEqualTo("{\"ativo\": true}");
        assertThat(query.getValue().getSortObject().toJson()).isEqualTo("{\"ordem\": 1, \"slug\": 1}");
        assertThat(query.getValue().getSkip()).isEqualTo(60L);
        assertThat(query.getValue().getLimit()).isEqualTo(21);
    }

    @Test
    @DisplayName("NX-04: a projeção inclui imagem e o documento com imagem vira chave+alt; sem imagem, nulos")
    void projecaoIncluiImagem() {
        Document comFoto = doc(1).append("imagem", new Document("chave", "item-1").append("alt", "Foto ilustrativa: item"));
        when(mongo.find(org.mockito.ArgumentMatchers.any(Query.class), eq(Document.class), eq("catalogo_loja")))
                .thenReturn(List.of(comFoto, doc(2)));

        FatiaCatalogo fatia = repositorio.listarAtivos(null, 0, 5);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Document.class), eq("catalogo_loja"));
        assertThat(query.getValue().getFieldsObject().keySet()).contains("imagem");
        assertThat(fatia.itens().get(0).imagemChave()).isEqualTo("item-1");
        assertThat(fatia.itens().get(0).imagemAlt()).isEqualTo("Foto ilustrativa: item");
        assertThat(fatia.itens().get(1).imagemChave()).isNull();
        assertThat(fatia.itens().get(1).imagemAlt()).isNull();
    }

    @Test
    @DisplayName("com categoria o filtro de categoria entra na consulta")
    void filtroDeCategoria() {
        when(mongo.find(org.mockito.ArgumentMatchers.any(Query.class), eq(Document.class), eq("catalogo_loja")))
                .thenReturn(List.of());

        repositorio.listarAtivos(CategoriaLoja.HIGIENE, 0, 10);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Document.class), eq("catalogo_loja"));
        assertThat(query.getValue().getQueryObject().toJson()).contains("\"categoria\": \"HIGIENE\"").contains("\"ativo\": true");
    }

    @Test
    @DisplayName("size+1 lidos => hasNext e devolve só size; skip de page máxima cabe em int")
    void fatiaComProxima() {
        List<Document> lidos = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            lidos.add(doc(i));
        }
        when(mongo.find(org.mockito.ArgumentMatchers.any(Query.class), eq(Document.class), eq("catalogo_loja")))
                .thenReturn(lidos);

        FatiaCatalogo fatia = repositorio.listarAtivos(null, 10000, 3);

        assertThat(fatia.temProxima()).isTrue();
        assertThat(fatia.itens()).hasSize(3);
        assertThat(fatia.itens().get(0).preco()).isEqualTo(100L);
        assertThat(fatia.itens().get(0).dados()).containsEntry("tipoHigiene", "ESCOVA");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Document.class), eq("catalogo_loja"));
        assertThat(query.getValue().getSkip()).isEqualTo(30000L);
    }

    @Test
    @DisplayName("exatamente size lidos => sem próxima")
    void fatiaSemProxima() {
        when(mongo.find(org.mockito.ArgumentMatchers.any(Query.class), eq(Document.class), eq("catalogo_loja")))
                .thenReturn(List.of(doc(1), doc(2)));

        FatiaCatalogo fatia = repositorio.listarAtivos(null, 0, 2);

        assertThat(fatia.temProxima()).isFalse();
        assertThat(fatia.itens()).hasSize(2);
    }

    @Test
    @DisplayName("m-6: preço ausente não vira 0; o documento é pulado e o WARN não traz conteúdo")
    void precoAusenteEPulado(org.springframework.boot.test.system.CapturedOutput saida) {
        Document semPreco = doc(1);
        semPreco.remove("preco");
        when(mongo.find(org.mockito.ArgumentMatchers.any(Query.class), eq(Document.class), eq("catalogo_loja")))
                .thenReturn(List.of(semPreco, doc(2)));

        FatiaCatalogo fatia = repositorio.listarAtivos(null, 0, 5);

        assertThat(fatia.itens()).extracting(i -> i.slug()).containsExactly("item-2");
        assertThat(saida.getAll()).contains("documento do catálogo inválido ignorado (slug=item-1)")
                .doesNotContain("Item 1");
    }

    @Test
    @DisplayName("m-6: categoria fora do enum é pulada sem lançar nem vazar nome de classe")
    void categoriaForaDoEnumEPulada(org.springframework.boot.test.system.CapturedOutput saida) {
        Document ruim = doc(1);
        ruim.put("categoria", "MEDICAMENTO");
        when(mongo.find(org.mockito.ArgumentMatchers.any(Query.class), eq(Document.class), eq("catalogo_loja")))
                .thenReturn(List.of(ruim, doc(2)));

        FatiaCatalogo fatia = repositorio.listarAtivos(null, 0, 5);

        assertThat(fatia.itens()).extracting(i -> i.slug()).containsExactly("item-2");
        assertThat(saida.getAll()).doesNotContain("No enum constant").doesNotContain("CategoriaLoja.");
    }
}
