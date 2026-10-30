package br.com.clyvo.kura.tutor.documento.application;

import br.com.clyvo.kura.tutor.documento.infra.DocumentoTutorMongoRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava por reflexão o escopo por titular: todo método público do repositório de documentos
 * (e das interfaces que ele herda, recursivamente) recebe {@code long idTutor} como primeiro parâmetro.
 * Um método novo sem idTutor quebra aqui em vez de virar leitura sem escopo.
 */
public class EscopoRepositorioReflexaoTest {

    /** Devolve "Tipo.metodo" de cada método público sem {@code long idTutor} na primeira posição. */
    public static List<String> metodosSemIdTutor(Class<?> tipo) {
        List<String> violacoes = new ArrayList<>();
        for (Class<?> t : tipoEHerancas(tipo)) {
            for (Method m : t.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers()) || m.isSynthetic() || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Parameter[] ps = m.getParameters();
                boolean ok = ps.length > 0 && ps[0].getType() == long.class
                        && ps[0].isNamePresent() && ps[0].getName().equals("idTutor");
                if (!ok) {
                    violacoes.add(t.getSimpleName() + "." + m.getName());
                }
            }
        }
        return violacoes;
    }

    private static Set<Class<?>> tipoEHerancas(Class<?> tipo) {
        Set<Class<?>> todos = new LinkedHashSet<>();
        coletar(tipo, todos);
        return todos;
    }

    private static void coletar(Class<?> tipo, Set<Class<?>> acumulado) {
        if (tipo == null || tipo == Object.class || !acumulado.add(tipo)) {
            return;
        }
        for (Class<?> i : tipo.getInterfaces()) {
            coletar(i, acumulado);
        }
        coletar(tipo.getSuperclass(), acumulado);
    }

    interface PaiSemEscopo {
        List<String> listarTudo(long idPet);
    }

    interface FilhoComEscopoMasHerdaBuraco extends PaiSemEscopo {
        void apagar(long idTutor, String id);
    }

    interface Correto {
        void apagar(long idTutor, String id);
    }

    @Test
    @DisplayName("o repositório de documentos e o que ele herda: todo método recebe long idTutor")
    void repositorioDeDocumentosTemEscopo() {
        assertThat(metodosSemIdTutor(DocumentoTutorRepositorio.class)).isEmpty();
        assertThat(metodosSemIdTutor(DocumentoTutorMongoRepositorio.class)).isEmpty();
    }

    @Test
    @DisplayName("controle positivo: o verificador acusa método sem idTutor, inclusive em interface herdada")
    void verificadorAcusaBuraco() {
        assertThat(metodosSemIdTutor(FilhoComEscopoMasHerdaBuraco.class)).containsExactly("PaiSemEscopo.listarTudo");
        assertThat(metodosSemIdTutor(Correto.class)).isEmpty();
    }

    @Test
    @DisplayName("pré-condição: o build guarda o nome dos parâmetros (-parameters), senão o verificador é cego")
    void nomesDeParametroPresentes() throws NoSuchMethodException {
        Method m = DocumentoTutorRepositorio.class.getMethod("listar", long.class, long.class);

        assertThat(m.getParameters()[0].isNamePresent()).isTrue();
    }
}
