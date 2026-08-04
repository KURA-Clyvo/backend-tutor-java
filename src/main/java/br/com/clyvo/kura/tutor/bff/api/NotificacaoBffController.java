package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.notificacao.api.dto.NotificacaoResponse;
import br.com.clyvo.kura.tutor.notificacao.application.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BFF notificações — expõe /api/v1/tutor/notificacoes com idTutor do JWT.
 *
 * Somente leitura: NOTIFICACAO é .NET owned (CLAUDE.md — "Tabelas e ownership").
 * TASK-31 avaliou o par PATCH .../{id}/lida e PATCH .../lidas (marcar como lida)
 * e decidiu NÃO implementá-los — exigiriam UPDATE numa tabela fora do domínio
 * Java. O app degrada essa marcação para estado local (ver INT-01-contract-map).
 */
@RestController
@RequestMapping("/v1/tutor/notificacoes")
@Tag(name = "Notificações BFF", description = "Superfície /api/v1/tutor/notificacoes/** para mobile-tutor-rn (somente leitura)")
@SecurityRequirement(name = "bearerAuth")
public class NotificacaoBffController {

    private final NotificacaoService notificacaoService;
    private final ContaTutorRepository contaTutorRepository;

    public NotificacaoBffController(NotificacaoService notificacaoService,
                                     ContaTutorRepository contaTutorRepository) {
        this.notificacaoService = notificacaoService;
        this.contaTutorRepository = contaTutorRepository;
    }

    @GetMapping
    @Operation(
        summary = "Lista notificações do tutor autenticado",
        description = "idTutor derivado do JWT. Paginado, mais recente primeiro. Leitura de NOTIFICACAO (.NET owned)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notificações retornadas"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "ContaTutor não encontrada")
    })
    public ResponseEntity<Page<NotificacaoResponse>> listar(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {
        Long idTutor = resolverIdTutor(auth);
        return ResponseEntity.ok(notificacaoService.listar(idTutor, pageable));
    }

    private Long resolverIdTutor(Authentication auth) {
        return contaTutorRepository
                .findIdTutorByEmail(auth.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("ContaTutor", auth.getName()));
    }
}
