package br.com.clyvo.kura.tutor.documento.application;

import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.PetRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Documentos do pet (RG, carteira de vacina, microchip) do tutor autenticado. O vínculo
 * tutor-pet é conferido ANTES de qualquer acesso ao Mongo. LGPD: nenhum log carrega conteúdo,
 * número do documento ou nome do arquivo enviado.
 */
@Slf4j
@Service
public class DocumentoTutorService {

    public static final int TETO_POR_PET = 20;
    public static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final Pattern NUMERO = Pattern.compile("^[A-Za-z0-9./-]{1,40}$");

    private final DocumentoTutorRepositorio repositorio;
    private final PetRepository petRepository;
    private final Clock clock;

    public DocumentoTutorService(DocumentoTutorRepositorio repositorio, PetRepository petRepository, Clock clock) {
        this.repositorio = repositorio;
        this.petRepository = petRepository;
        this.clock = clock;
    }

    public DocumentoTutor criar(long idTutor, long idPet, TipoDocumento tipo, EmissorDocumento emissor,
                                String numero, byte[] conteudo) {
        garantirPetDoTutor(idTutor, idPet);
        if (conteudo == null || conteudo.length == 0) {
            throw new DocumentoInvalidoException("Arquivo vazio.");
        }
        if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoGrandeException();
        }
        if (numero != null && !NUMERO.matcher(numero).matches()) {
            throw new DocumentoInvalidoException(
                    "Número inválido: use até 40 letras, dígitos, ponto, barra ou hífen, sem espaços.");
        }
        String contentType = AssinaturaArquivo.detectar(conteudo)
                .orElseThrow(TipoArquivoNaoSuportadoException::new);
        if (repositorio.contar(idTutor, idPet) >= TETO_POR_PET) {
            throw new LimiteDocumentosException(TETO_POR_PET);
        }
        NovoDocumento novo = new NovoDocumento(idPet, tipo, emissor, numero, conteudo, contentType,
                sha256(conteudo), clock.instant());
        DocumentoTutor gravado = repositorio.inserir(idTutor, novo);
        log.info("Documento do pet gravado (idPet={}, tipo={})", idPet, tipo);
        return gravado;
    }

    public List<DocumentoTutor> listar(long idTutor, long idPet) {
        garantirPetDoTutor(idTutor, idPet);
        return repositorio.listar(idTutor, idPet);
    }

    public ArquivoDocumento abrir(long idTutor, long idPet, String idDocumento) {
        garantirPetDoTutor(idTutor, idPet);
        return repositorio.abrirArquivo(idTutor, idPet, idDocumento)
                .orElseThrow(DocumentoTutorService::documentoNaoEncontrado);
    }

    /**
     * LGPD art. 18 VI: o titular elimina o proprio documento mesmo sem vinculo ativo ou com o pet
     * inativo. Sem checagem de vinculo de proposito: o filtro {_id, idTutor, idPet} do repositorio
     * ja impede tocar dado alheio (outro tutor ou outro pet = 404).
     */
    public void apagar(long idTutor, long idPet, String idDocumento) {
        if (!repositorio.apagar(idTutor, idPet, idDocumento)) {
            throw documentoNaoEncontrado();
        }
        log.info("Documento do pet apagado (idPet={})", idPet);
    }

    /** 403 sem vínculo; 404 pet inativo. Roda antes de qualquer chamada ao repositório de documentos. */
    private void garantirPetDoTutor(long idTutor, long idPet) {
        if (petRepository.countVinculo(idPet, idTutor) == 0) {
            throw new ForbiddenException("Acesso negado: pet não pertence ao tutor autenticado.");
        }
        petRepository.findByIdPetAndStAtivo(idPet, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pet", idPet));
    }

    /** 404 de documento: mesma resposta para "não existe" e "é de outro tutor". */
    private static RecursoNaoEncontradoException documentoNaoEncontrado() {
        return new RecursoNaoEncontradoException("Documento não encontrado.");
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
