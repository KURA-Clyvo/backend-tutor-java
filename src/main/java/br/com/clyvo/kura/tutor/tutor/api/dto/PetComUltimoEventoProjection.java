package br.com.clyvo.kura.tutor.tutor.api.dto;

import java.time.LocalDateTime;

/**
 * Projection combining a pet's identity with the date of its most recent clinical
 * event (drawn from VW_TIMELINE_PET). A null dtUltimoEvento means no events yet.
 * Used by the tutor dashboard to surface pet-activity at a glance.
 */
public record PetComUltimoEventoProjection(
    Long idPet,
    String nmPet,
    LocalDateTime dtUltimoEvento
) {}
