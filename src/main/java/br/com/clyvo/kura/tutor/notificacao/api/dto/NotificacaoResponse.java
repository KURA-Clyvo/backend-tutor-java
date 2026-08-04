package br.com.clyvo.kura.tutor.notificacao.api.dto;

import br.com.clyvo.kura.tutor.notificacao.domain.Notificacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Notificação do tutor — leitura de NOTIFICACAO (.NET owned)")
public record NotificacaoResponse(
        @Schema(description = "ID da notificação", example = "1") Long idNotificacao,
        @Schema(description = "Título", example = "Vacina próxima do vencimento") String dsTitulo,
        @Schema(description = "Mensagem", example = "A vacina antirrábica de Marley vence em 5 dias.") String dsMensagem,
        @Schema(description = "Data/hora de criação") LocalDateTime dtCriacao,
        @Schema(description = "true = já lida (ST_LIDA='S')") boolean flLida
) {
    public static NotificacaoResponse fromEntity(Notificacao n) {
        return new NotificacaoResponse(
                n.getIdNotificacao(),
                n.getDsTitulo(),
                n.getDsMensagem(),
                n.getDtCriacao(),
                n.isLida());
    }
}
