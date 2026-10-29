package br.com.clyvo.kura.tutor.documento.application;

import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência de documentos_tutor. TODO método recebe {@code long idTutor}: o escopo por
 * titular é obrigatório no tipo, não opcional (travado por teste de reflexão).
 */
public interface DocumentoTutorRepositorio {

    DocumentoTutor inserir(long idTutor, NovoDocumento novo);

    /** Do mais novo para o mais antigo, sem os bytes. */
    List<DocumentoTutor> listar(long idTutor, long idPet);

    long contar(long idTutor, long idPet);

    Optional<ArquivoDocumento> abrirArquivo(long idTutor, long idPet, String idDocumento);

    /** @return true se removeu (exclusão física, LGPD art. 18 VI); false se não existe para este tutor. */
    boolean apagar(long idTutor, long idPet, String idDocumento);
}
