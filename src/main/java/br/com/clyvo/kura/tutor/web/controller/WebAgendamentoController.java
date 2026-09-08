// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoResponse;
import br.com.clyvo.kura.tutor.agendamento.api.dto.AgendamentoUpdateRequest;
import br.com.clyvo.kura.tutor.agendamento.application.AgendamentoService;
import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import br.com.clyvo.kura.tutor.web.form.CancelarAgendamentoForm;
import br.com.clyvo.kura.tutor.web.form.RemarcarAgendamentoForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Fluxo não-CRUD "agendamento com lock otimista" (SJ3-06, branch
 * descartável — ver {@code docs/ADR-web.md}). Lista os agendamentos do
 * TUTOR autenticado e oferece cancelar (com motivo) e remarcar; o
 * {@code NR_VERSION} do remarcar produz 409 em conflito de concorrência —
 * é o que separa este fluxo de um CRUD simples (backlog, seção `SJ3-06`).
 *
 * Reusa a API de domínio existente ({@code AgendamentoService.listar} —
 * {@code AgendamentoService.java:48} — {@code .atualizar} — {@code :97} —
 * e {@code .cancelar} — {@code :140}) sem alterar nenhuma linha dela.
 * {@code authentication.getName()} é passado direto como
 * {@code emailAutenticado}: é o mesmo campo
 * ({@code conta.getDsEmailLogin()}) que
 * {@code WebAuthenticationProvider.autenticarTutor} usa para montar o
 * principal do chain {@code /web/**} — não há resolução de tutor nova
 * aqui (ver {@code sj3-06-report.md}, repo de planejamento).
 *
 * <p><b>Armadilha fechada pelos {@code @ExceptionHandler} abaixo</b> — ver
 * {@link br.com.clyvo.kura.tutor.shared.exception.GlobalExceptionHandler},
 * que é {@code @RestControllerAdvice} <b>sem</b> {@code basePackages}/
 * {@code assignableTypes}: cobre TODOS os controllers, inclusive este. Sem
 * os handlers locais, {@code ObjectOptimisticLockingFailureException}/
 * {@code ForbiddenException}/{@code NotFoundException}/
 * {@code RegraDeNegocioException} lançadas por {@code AgendamentoService}
 * escapariam para o advice de PRODUTO e o tutor receberia JSON cru em vez
 * da página — mesma família do achado G2-A que a fix wave da SJ3-05
 * fechou em {@code Web403Controller}. Um {@code @ExceptionHandler} de
 * instância (deste {@code @Controller}) tem precedência sobre um
 * {@code @RestControllerAdvice} de produto — é essa precedência que faz a
 * diferença aqui. Prova HTTP literal (com e sem os handlers, para provar
 * que são eles que fazem a diferença): {@code sj3-06-report.md}.
 */
@Controller
@RequestMapping("/web/agendamentos")
public class WebAgendamentoController {

    private final AgendamentoService agendamentoService;

    public WebAgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        model.addAttribute("agendamentos", buscarTodos(authentication));
        return "web/agendamentos";
    }

    @GetMapping("/{id}/cancelar")
    public String formCancelar(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("agendamento", buscarUm(id, authentication));
        model.addAttribute("cancelarForm", new CancelarAgendamentoForm());
        return "web/agendamento-cancelar";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                            @Valid @ModelAttribute("cancelarForm") CancelarAgendamentoForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("agendamento", buscarUm(id, authentication));
            return "web/agendamento-cancelar";
        }
        agendamentoService.cancelar(authentication.getName(), id, form.getMotivo());
        redirectAttributes.addFlashAttribute("mensagem", "Agendamento cancelado com sucesso.");
        return "redirect:/web/agendamentos";
    }

    @GetMapping("/{id}/remarcar")
    public String formRemarcar(@PathVariable Long id, Authentication authentication, Model model) {
        AgendamentoResponse agendamento = buscarUm(id, authentication);
        RemarcarAgendamentoForm form = new RemarcarAgendamentoForm();
        form.setNrVersion(agendamento.nrVersion());
        model.addAttribute("agendamento", agendamento);
        model.addAttribute("remarcarForm", form);
        return "web/agendamento-remarcar";
    }

    @PostMapping("/{id}/remarcar")
    public String remarcar(@PathVariable Long id,
                            @Valid @ModelAttribute("remarcarForm") RemarcarAgendamentoForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("agendamento", buscarUm(id, authentication));
            return "web/agendamento-remarcar";
        }
        AgendamentoUpdateRequest request = new AgendamentoUpdateRequest(
                form.getDtAgendamento(), null, null, null, form.getNrVersion());
        agendamentoService.atualizar(authentication.getName(), id, request);
        redirectAttributes.addFlashAttribute("mensagem", "Agendamento remarcado com sucesso.");
        return "redirect:/web/agendamentos";
    }

    private List<AgendamentoResponse> buscarTodos(Authentication authentication) {
        return agendamentoService.listar(authentication.getName(), null, null, null, null,
                        PageRequest.of(0, 200, Sort.by("dtAgendamento").ascending()))
                .getContent();
    }

    private AgendamentoResponse buscarUm(Long id, Authentication authentication) {
        return buscarTodos(authentication).stream()
                .filter(a -> a.idAgendamento().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Agendamento", id));
    }

    // ─── 409 — conflito de versão (lock otimista, a cena central da task) ──

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflitoDeVersao(Model model) {
        model.addAttribute("titulo", "Conflito de atualização");
        model.addAttribute("mensagemErro",
                "Alguém alterou este agendamento enquanto você editava. Recarregue a página e "
                        + "tente novamente.");
        return "web/agendamento-erro";
    }

    // ─── 403 / 404 / 422 — mesmas exceções de domínio que o advice de produto
    // mapearia para JSON; aqui viram página, com a mensagem do próprio domínio.

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String proibido(ForbiddenException ex, Model model) {
        model.addAttribute("titulo", "Acesso negado");
        model.addAttribute("mensagemErro", ex.getMessage());
        return "web/agendamento-erro";
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(NotFoundException ex, Model model) {
        model.addAttribute("titulo", "Agendamento não encontrado");
        model.addAttribute("mensagemErro", ex.getMessage());
        return "web/agendamento-erro";
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String regraDeNegocio(RegraDeNegocioException ex, Model model) {
        model.addAttribute("titulo", "Não foi possível concluir");
        model.addAttribute("mensagemErro", ex.getMessage());
        return "web/agendamento-erro";
    }
}
