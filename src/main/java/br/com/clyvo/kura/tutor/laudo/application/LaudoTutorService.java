package br.com.clyvo.kura.tutor.laudo.application;

import br.com.clyvo.kura.tutor.shared.pet.GuardaPetDoTutor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Laudos do pet para o tutor autenticado. O vínculo tutor-pet é conferido ANTES do Mongo. */
@Service
public class LaudoTutorService {

    private final LaudoLeituraRepositorio repositorio;
    private final GuardaPetDoTutor guarda;

    public LaudoTutorService(LaudoLeituraRepositorio repositorio, GuardaPetDoTutor guarda) {
        this.repositorio = repositorio;
        this.guarda = guarda;
    }

    public List<LaudoTutor> listar(long idTutor, long idPet) {
        guarda.garantir(idTutor, idPet);
        return repositorio.listarAtivos(idPet);
    }
}
