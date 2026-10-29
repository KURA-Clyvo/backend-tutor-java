package br.com.clyvo.kura.tutor.documento;

import br.com.clyvo.kura.tutor.documento.application.ArquivoDocumento;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutor;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutorRepositorio;
import br.com.clyvo.kura.tutor.documento.application.NovoDocumento;
import com.mongodb.MongoTimeoutException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Fake em memória escopado por tutor (HTTP dos documentos): documento de outro tutor não existe. */
public class RepositorioDocumentosEmMemoria implements DocumentoTutorRepositorio {

    private record Linha(long idTutor, DocumentoTutor meta, byte[] conteudo) {
    }

    private final List<Linha> linhas = new ArrayList<>();
    private final AtomicLong sequencia = new AtomicLong(1);
    private boolean indisponivel;

    public void indisponivel(boolean valor) {
        this.indisponivel = valor;
    }

    public void limpar() {
        linhas.clear();
        indisponivel = false;
    }

    public int total() {
        return linhas.size();
    }

    private void checar() {
        if (indisponivel) {
            throw new MongoTimeoutException("simulado");
        }
    }

    @Override
    public DocumentoTutor inserir(long idTutor, NovoDocumento n) {
        checar();
        String id = String.format("%024x", sequencia.getAndIncrement());
        DocumentoTutor meta = new DocumentoTutor(id, n.idPet(), n.tipo(), n.emissor(), n.numero(),
                n.contentType(), n.conteudo().length, n.criadoEm());
        linhas.add(new Linha(idTutor, meta, n.conteudo()));
        return meta;
    }

    @Override
    public List<DocumentoTutor> listar(long idTutor, long idPet) {
        checar();
        return linhas.stream()
                .filter(l -> l.idTutor == idTutor && l.meta.idPet() == idPet)
                .map(Linha::meta)
                .sorted(Comparator.comparing(DocumentoTutor::criadoEm).reversed())
                .toList();
    }

    @Override
    public long contar(long idTutor, long idPet) {
        checar();
        return linhas.stream().filter(l -> l.idTutor == idTutor && l.meta.idPet() == idPet).count();
    }

    @Override
    public Optional<ArquivoDocumento> abrirArquivo(long idTutor, long idPet, String idDocumento) {
        checar();
        return linhas.stream()
                .filter(l -> l.idTutor == idTutor && l.meta.idPet() == idPet && l.meta.id().equals(idDocumento))
                .findFirst()
                .map(l -> new ArquivoDocumento(l.meta.contentType(), l.conteudo));
    }

    @Override
    public boolean apagar(long idTutor, long idPet, String idDocumento) {
        checar();
        return linhas.removeIf(l -> l.idTutor == idTutor && l.meta.idPet() == idPet && l.meta.id().equals(idDocumento));
    }
}
