package br.com.clyvo.kura.tutor.onboarding.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados resumidos do tutor retornados após cadastro por convite")
public record TutorResumoResponse(
        @Schema(example = "1")             Long   idTutor,
        @Schema(example = "Felipe Ferretel") String nmTutor
) {}
