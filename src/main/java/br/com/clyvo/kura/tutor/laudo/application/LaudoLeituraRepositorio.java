package br.com.clyvo.kura.tutor.laudo.application;

import java.util.List;

/** Porta de LEITURA dos laudos do pet. Nenhum método de escrita: a coleção é da .NET. */
public interface LaudoLeituraRepositorio {

    int TETO_LEITURA = 100;

    /** Laudos ativos do pet, {@code dataExame} decrescente, no máximo {@link #TETO_LEITURA}. */
    List<LaudoTutor> listarAtivos(long idPet);
}
