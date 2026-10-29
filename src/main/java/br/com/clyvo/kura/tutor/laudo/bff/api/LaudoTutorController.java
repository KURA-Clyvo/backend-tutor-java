package br.com.clyvo.kura.tutor.laudo.bff.api;

import br.com.clyvo.kura.tutor.laudo.application.LaudoTutorService;
import br.com.clyvo.kura.tutor.laudo.bff.api.dto.LaudoTutorResponse;
import br.com.clyvo.kura.tutor.laudo.bff.api.dto.ListaLaudosResponse;
import br.com.clyvo.kura.tutor.laudo.infra.GeradorUrlLaudo;
import br.com.clyvo.kura.tutor.shared.security.TutorAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Laudos do pet para o tutor (leitura de {@code laudos}, dona .NET). idTutor SEMPRE do JWT. */
@RestController
@RequestMapping("/v1/tutor/pets/{idPet}/laudos")
@Tag(name = "Laudos do pet", description = "Laudos registrados pela clínica, somente leitura (MongoDB, contrato laudos v1)")
@SecurityRequirement(name = "bearerAuth")
public class LaudoTutorController {

    private final LaudoTutorService service;
    private final GeradorUrlLaudo geradorUrl;
    private final TutorAutenticado tutorAutenticado;

    public LaudoTutorController(LaudoTutorService service, GeradorUrlLaudo geradorUrl,
                                TutorAutenticado tutorAutenticado) {
        this.service = service;
        this.geradorUrl = geradorUrl;
        this.tutorAutenticado = tutorAutenticado;
    }

    @GetMapping
    @Operation(summary = "Lista os laudos ativos do pet",
            description = "dataExame decrescente. urlArquivo é uma URL assinada de curta duração, ou null quando indisponível. "
                    + "403 pet de outro tutor, 404 pet inativo, 503 LAUDOS_INDISPONIVEL.")
    public ResponseEntity<ListaLaudosResponse> listar(Authentication auth, @PathVariable long idPet) {
        var itens = service.listar(tutorAutenticado.idTutor(auth), idPet).stream()
                .map(l -> LaudoTutorResponse.de(l, geradorUrl.gerarUrl(idPet, l.chaveArquivo())))
                .toList();
        return ResponseEntity.ok(new ListaLaudosResponse(itens));
    }
}
