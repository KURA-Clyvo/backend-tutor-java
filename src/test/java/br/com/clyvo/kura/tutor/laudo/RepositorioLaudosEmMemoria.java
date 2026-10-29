package br.com.clyvo.kura.tutor.laudo;

import br.com.clyvo.kura.tutor.laudo.application.LaudoLeituraRepositorio;
import br.com.clyvo.kura.tutor.laudo.application.LaudoTutor;
import com.mongodb.MongoTimeoutException;

import java.util.ArrayList;
import java.util.List;

/** Fake em memória de leitura de laudos (HTTP): só devolve o que foi semeado para o pet. */
public class RepositorioLaudosEmMemoria implements LaudoLeituraRepositorio {

    private final List<LaudoTutor> linhas = new ArrayList<>();
    private final List<Long> pets = new ArrayList<>();
    private boolean indisponivel;

    public void limpar() {
        linhas.clear();
        pets.clear();
        indisponivel = false;
    }

    public void indisponivel(boolean valor) {
        this.indisponivel = valor;
    }

    public void semear(long idPet, LaudoTutor laudo) {
        pets.add(idPet);
        linhas.add(laudo);
    }

    @Override
    public List<LaudoTutor> listarAtivos(long idPet) {
        if (indisponivel) {
            throw new MongoTimeoutException("simulado");
        }
        List<LaudoTutor> out = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            if (pets.get(i) == idPet) {
                out.add(linhas.get(i));
            }
        }
        return out;
    }
}
