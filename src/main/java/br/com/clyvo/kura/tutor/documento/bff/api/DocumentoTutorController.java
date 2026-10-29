package br.com.clyvo.kura.tutor.documento.bff.api;

import br.com.clyvo.kura.tutor.shared.security.TutorAutenticado;
import br.com.clyvo.kura.tutor.documento.application.ArquivoDocumento;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutor;
import br.com.clyvo.kura.tutor.documento.application.DocumentoTutorService;
import br.com.clyvo.kura.tutor.documento.application.EmissorDocumento;
import br.com.clyvo.kura.tutor.documento.application.TipoDocumento;
import br.com.clyvo.kura.tutor.documento.bff.api.dto.DocumentoResponse;
import br.com.clyvo.kura.tutor.documento.bff.api.dto.ListaDocumentosResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * BFF dos documentos do pet (RG, carteira de vacina, microchip). idTutor SEMPRE do JWT; o pet
 * do path só identifica o recurso e o vínculo é conferido no service antes do Mongo.
 */
@RestController
@RequestMapping("/v1/tutor/pets/{idPet}/documentos")
@Tag(name = "Documentos do pet", description = "Upload e consulta de documentos do pet (MongoDB, contrato documentos_tutor v1)")
@SecurityRequirement(name = "bearerAuth")
public class DocumentoTutorController {

    private final DocumentoTutorService service;
    private final TutorAutenticado tutorAutenticado;

    public DocumentoTutorController(DocumentoTutorService service, TutorAutenticado tutorAutenticado) {
        this.service = service;
        this.tutorAutenticado = tutorAutenticado;
    }

    @PostMapping
    @Operation(summary = "Envia um documento do pet",
            description = "multipart/form-data: arquivo (JPEG, PNG, WEBP ou PDF, até 5 MB), tipo, emissor? e numero?. "
                    + "O tipo do arquivo é detectado pela assinatura, não pelo Content-Type do cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Documento gravado"),
        @ApiResponse(responseCode = "400", description = "Validação"),
        @ApiResponse(responseCode = "403", description = "Pet de outro tutor"),
        @ApiResponse(responseCode = "404", description = "Pet inativo"),
        @ApiResponse(responseCode = "409", description = "Teto de 20 documentos por pet"),
        @ApiResponse(responseCode = "413", description = "Arquivo acima de 5 MB"),
        @ApiResponse(responseCode = "415", description = "Assinatura do arquivo não reconhecida ou Content-Type não multipart"),
        @ApiResponse(responseCode = "503", description = "DOCUMENTOS_INDISPONIVEL")
    })
    public ResponseEntity<DocumentoResponse> enviar(
            Authentication auth,
            HttpServletRequest request,
            @PathVariable long idPet,
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestParam("tipo") TipoDocumento tipo,
            @RequestParam(value = "emissor", required = false) EmissorDocumento emissor,
            @RequestParam(value = "numero", required = false) String numero) throws IOException {
        long idTutor = tutorAutenticado.idTutor(auth);
        String numeroLimpo = numero == null || numero.isBlank() ? null : numero;
        DocumentoTutor criado = service.criar(idTutor, idPet, tipo, emissor, numeroLimpo, arquivo.getBytes());
        DocumentoResponse corpo = DocumentoResponse.de(criado, request.getContextPath());
        return ResponseEntity.created(URI.create(corpo.urlArquivo())).body(corpo);
    }

    @GetMapping
    @Operation(summary = "Lista os documentos do pet (sem os bytes)")
    public ResponseEntity<ListaDocumentosResponse> listar(
            Authentication auth, HttpServletRequest request, @PathVariable long idPet) {
        List<DocumentoResponse> itens = service.listar(tutorAutenticado.idTutor(auth), idPet).stream()
                .map(d -> DocumentoResponse.de(d, request.getContextPath()))
                .toList();
        return ResponseEntity.ok(new ListaDocumentosResponse(itens));
    }

    @GetMapping("/{idDocumento}/arquivo")
    @Operation(summary = "Baixa o arquivo do documento",
            description = "Content-Type gravado no upload; nosniff (X-Content-Type-Options, escrito pelo Spring Security); sem cache. Documento de outro tutor: 404.")
    public ResponseEntity<byte[]> arquivo(Authentication auth, @PathVariable long idPet,
                                          @PathVariable String idDocumento) {
        ArquivoDocumento arquivo = service.abrir(tutorAutenticado.idTutor(auth), idPet, idDocumento);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().cachePrivate().getHeaderValue())
                .body(arquivo.conteudo());
    }

    @DeleteMapping("/{idDocumento}")
    @Operation(summary = "Apaga o documento (exclusão física, LGPD art. 18 VI)")
    public ResponseEntity<Void> apagar(Authentication auth, @PathVariable long idPet,
                                       @PathVariable String idDocumento) {
        service.apagar(tutorAutenticado.idTutor(auth), idPet, idDocumento);
        return ResponseEntity.noContent().build();
    }

}
