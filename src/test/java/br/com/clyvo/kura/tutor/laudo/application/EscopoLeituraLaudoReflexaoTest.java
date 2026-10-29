package br.com.clyvo.kura.tutor.laudo.application;

import br.com.clyvo.kura.tutor.laudo.infra.LaudoMongoLeitura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava por reflexão: a porta de laudos é SÓ de leitura e todo método recebe {@code long idPet}
 * como primeiro parâmetro (interfaces herdadas incluídas). Escopo de clínica não existe aqui por
 * contrato: o escopo do tutor é o pet.
 */
class EscopoLeituraLaudoReflexaoTest {

    private static final Pattern ESCRITA = Pattern.compile(
            "^(salvar|inserir|atualizar|apagar|remover|delete|save|insert|update).*", Pattern.CASE_INSENSITIVE);

    /** "Tipo.metodo" de cada método público sem long idPet na 1a posição OU com nome de escrita. */
    static List<String> violacoes(Class<?> tipo) {
        List<String> out = new ArrayList<>();
        for (Class<?> t : tipoEHerancas(tipo)) {
            for (Method m : t.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers()) || m.isSynthetic() || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Parameter[] ps = m.getParameters();
                boolean escopado = ps.length > 0 && ps[0].getType() == long.class
                        && ps[0].isNamePresent() && ps[0].getName().equals("idPet");
                if (!escopado || ESCRITA.matcher(m.getName()).matches()) {
                    out.add(t.getSimpleName() + "." + m.getName());
                }
            }
        }
        return out;
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
        List<String> listarTudo();
    }

    interface FilhoEscopadoMasHerdaBuraco extends PaiSemEscopo {
        List<String> listar(long idPet);
    }

    interface ComEscrita {
        void salvarLaudo(long idPet, String x);
    }

    interface Correto {
        List<String> listar(long idPet);
    }

    @Test
    @DisplayName("a porta de laudos e a implementação Mongo: todo método recebe long idPet e nenhum escreve")
    void portaDeLaudosEhSoLeituraEscopada() {
        assertThat(violacoes(LaudoLeituraRepositorio.class)).isEmpty();
        assertThat(violacoes(LaudoMongoLeitura.class)).isEmpty();
    }

    @Test
    @DisplayName("controle positivo: acusa método sem idPet (inclusive herdado) e método com nome de escrita")
    void verificadorAcusa() {
        assertThat(violacoes(FilhoEscopadoMasHerdaBuraco.class)).containsExactly("PaiSemEscopo.listarTudo");
        assertThat(violacoes(ComEscrita.class)).containsExactly("ComEscrita.salvarLaudo");
        assertThat(violacoes(Correto.class)).isEmpty();
    }

    @Test
    @DisplayName("pré-condição: o build guarda o nome dos parâmetros (-parameters), senão o verificador é cego")
    void nomesDeParametroPresentes() throws NoSuchMethodException {
        Method m = LaudoLeituraRepositorio.class.getMethod("listarAtivos", long.class);

        assertThat(m.getParameters()[0].isNamePresent()).isTrue();
    }
}
