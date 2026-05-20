package br.com.clyvo.kura.tutor.timeline.api;

import br.com.clyvo.kura.tutor.timeline.api.dto.TimelineEventoResponse;
import br.com.clyvo.kura.tutor.timeline.api.dto.VacinaVencendoResponse;
import br.com.clyvo.kura.tutor.timeline.application.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Timeline", description = "Histórico clínico e vacinas vencendo — tutor só acessa seus próprios dados")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/pets/{idPet}/timeline")
    @Operation(summary = "Linha do tempo de atendimentos do pet (paginada, sort dtEvento desc)")
    public ResponseEntity<Page<TimelineEventoResponse>> listarTimeline(
            @PathVariable Long idPet,
            @PageableDefault(size = 20, sort = "dtEvento", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.listarTimeline(idPet, auth.getName(), pageable));
    }

    @GetMapping("/tutores/{idTutor}/vacinas-vencendo")
    @Operation(summary = "Vacinas agendadas nos próximos 30 dias para os pets do tutor")
    public ResponseEntity<List<VacinaVencendoResponse>> listarVacinasVencendo(
            @PathVariable Long idTutor,
            Authentication auth) {
        return ResponseEntity.ok(timelineService.listarVacinasVencendo(idTutor, auth.getName()));
    }
}
