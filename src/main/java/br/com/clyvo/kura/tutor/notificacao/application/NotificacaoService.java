package br.com.clyvo.kura.tutor.notificacao.application;

import br.com.clyvo.kura.tutor.notificacao.api.dto.NotificacaoResponse;
import br.com.clyvo.kura.tutor.notificacao.domain.repository.NotificacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOTIFICACAO é .NET owned (ver CLAUDE.md). Este serviço é estritamente
 * somente-leitura — nunca adicionar um método de escrita aqui. Marcar como
 * lida (PATCH) foi avaliado (TASK-31) e descartado: exigiria UPDATE numa
 * tabela que não pertence ao domínio Java.
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificacaoResponse> listar(Long idTutor, Pageable pageable) {
        return notificacaoRepository.findByIdTutorOrderByDtCriacaoDesc(idTutor, pageable)
                .map(NotificacaoResponse::fromEntity);
    }
}
