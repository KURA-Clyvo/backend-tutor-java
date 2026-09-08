// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.onboarding.domain.InviteTutor;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import br.com.clyvo.kura.tutor.web.domain.InviteTutorWebRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Painel de busca de conta (SJ3-07) — exclusivo do perfil SUPORTE
 * ({@code .hasRole("SUPORTE")} em
 * {@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig}). Busca uma
 * pessoa por e-mail e mostra o estado da conta ({@code nr_tentativas_login},
 * {@code dt_bloqueio}, {@code st_ativa}), oferecendo destravar.
 *
 * <p><b>A busca aceita DOIS caminhos</b> (§2.2 do brief): primeiro tenta o
 * e-mail como LOGIN de conta ({@link ContaTutorRepository#findByDsEmailLogin}),
 * e se não achar cai para e-mail de TUTOR
 * ({@link TutorRepository#findByDsEmailAndStAtivo}). Sem isso, uma busca só
 * por login nunca encontraria a metade "convite pendente" da demonstração —
 * o seed de dev dá conta a um tutor e convite a outro (ver
 * {@code afterMigrate__seeds_dev.sql}, seções 6/7/12).</p>
 *
 * <p><b>O convite é SOMENTE LEITURA</b> (§2.1 do brief) — não existe canal de
 * envio (e-mail/SMS/push) neste backend, então esta tela NUNCA promete
 * reenviar nada; mostra o convite pendente (token, canal, expiração) para o
 * suporte repassar por fora do sistema. Achado pelo {@code InviteTutorRepository}
 * de produto não servir (só {@code findByNrToken}): {@link InviteTutorWebRepository},
 * um repositório de leitura LOCAL DA BRANCH sobre a mesma entidade
 * ({@code InviteTutor}, {@code @Immutable}), busca por {@code idTutor}.</p>
 *
 * <p><b>O destrave usa os SETTERS públicos</b> de {@code ContaTutor}
 * ({@code setNrTentativasLogin(0)} + {@code setDtBloqueio(null)}), NUNCA
 * {@code registrarLoginSucesso()} (carimba {@code dtUltimoLogin} — falsificaria
 * auditoria de login) nem {@code limparBloqueioExpirado(...)} (é NO-OP em
 * bloqueio recente — o único caso em que o suporte é chamado; §8.2 do brief) nem
 * {@code resetarTentativas()} ({@code @Deprecated}, aponta para
 * {@code registrarLoginSucesso()}, que para este caso de uso é o conselho
 * errado — não "consertado" aqui por ser arquivo de produto; achado registrado
 * para a SJ3-08 recolher).</p>
 *
 * <p><b>Erro renderizado como página, não JSON</b> (§4 do brief): igual a
 * {@link WebAgendamentoController}, {@code @ExceptionHandler} LOCAL a este
 * {@code @Controller} tem precedência sobre o {@code GlobalExceptionHandler}
 * de produto ({@code @RestControllerAdvice} sem escopo, cobre {@code /web/**}
 * também). Reusa {@code templates/web/erro.html}, renomeada de
 * {@code agendamento-erro.html} nesta mesma task (era exclusiva de
 * {@link WebAgendamentoController}).</p>
 */
@Controller
@RequestMapping("/web/suporte")
public class SuporteController {

    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ContaTutorRepository contaTutorRepository;
    private final TutorRepository tutorRepository;
    private final InviteTutorWebRepository inviteTutorWebRepository;

    public SuporteController(ContaTutorRepository contaTutorRepository,
                              TutorRepository tutorRepository,
                              InviteTutorWebRepository inviteTutorWebRepository) {
        this.contaTutorRepository = contaTutorRepository;
        this.tutorRepository = tutorRepository;
        this.inviteTutorWebRepository = inviteTutorWebRepository;
    }

    @GetMapping("/contas")
    public String contas(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        if (email != null && !email.isBlank()) {
            if (!EMAIL_VALIDO.matcher(email).matches()) {
                model.addAttribute("mensagemBusca", "Informe um e-mail em formato válido.");
            } else {
                Optional<Resultado> resultado = buscar(email);
                if (resultado.isPresent()) {
                    model.addAttribute("resultado", resultado.get());
                } else {
                    model.addAttribute("mensagemBusca", "Nenhuma pessoa encontrada com este e-mail.");
                }
            }
        }
        return "web/suporte-contas";
    }

    /**
     * Destrava a conta zerando {@code nrTentativasLogin} e {@code dtBloqueio}
     * pelos setters públicos — NUNCA {@code registrarLoginSucesso()} (ver
     * javadoc da classe). {@code email} volta na URL do redirect só para
     * reexibir a mesma busca — não é usado para localizar a conta (o
     * destrave é sempre pelo {@code idConta} do formulário).
     */
    @PostMapping("/contas/destravar")
    public String destravar(@RequestParam Long idConta,
                             @RequestParam String email,
                             RedirectAttributes redirectAttributes) {
        ContaTutor conta = contaTutorRepository.findById(idConta)
                .orElseThrow(() -> new NotFoundException("Conta", idConta));
        conta.setNrTentativasLogin(0);
        conta.setDtBloqueio(null);
        contaTutorRepository.save(conta);
        redirectAttributes.addFlashAttribute("mensagem", "Conta destravada com sucesso.");
        redirectAttributes.addAttribute("email", email);
        return "redirect:/web/suporte/contas";
    }

    private Optional<Resultado> buscar(String email) {
        Optional<ContaTutor> contaOpt = contaTutorRepository.findByDsEmailLogin(email);
        ContaTutor conta = contaOpt.orElse(null);
        Long idTutor;
        if (contaOpt.isPresent()) {
            // Projeção dedicada (não conta.getTutor()): ContaTutor.tutor é FetchType.LAZY e
            // open-in-view é false (application.yml:17) — acessar o relacionamento aqui fora da
            // transação do repositório lançaria LazyInitializationException.
            idTutor = contaTutorRepository.findIdTutorByEmail(email).orElse(null);
        } else {
            idTutor = tutorRepository.findByDsEmailAndStAtivo(email, "S")
                    .map(Tutor::getIdTutor)
                    .orElse(null);
        }
        if (idTutor == null) {
            return Optional.empty();
        }
        Tutor tutor = tutorRepository.findByIdTutorAndStAtivo(idTutor, "S").orElse(null);
        if (tutor == null) {
            return Optional.empty();
        }
        InviteInfo convite = buscarConvitePendente(idTutor);
        return Optional.of(new Resultado(tutor, conta, convite));
    }

    private InviteInfo buscarConvitePendente(Long idTutor) {
        List<InviteTutor> convites = inviteTutorWebRepository.findByIdTutor(idTutor);
        return convites.stream()
                .filter(i -> i.isAtivo() && !i.isUtilizado())
                .max(Comparator.comparing(InviteTutor::getIdInvite))
                .map(i -> new InviteInfo(i.getNrToken(), i.getDsCanal(), i.getDtExpiracao(), i.isExpirado()))
                .orElse(null);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(NotFoundException ex, Model model) {
        model.addAttribute("titulo", "Não encontrado");
        model.addAttribute("mensagemErro", ex.getMessage());
        return "web/erro";
    }

    /** Convite pendente da pessoa — {@code expirado} deriva de {@link InviteTutor#isExpirado()}. */
    public record InviteInfo(String token, String canal, LocalDateTime dtExpiracao, boolean expirado) {}

    /** {@code conta}/{@code convite} nulos são estados válidos e discriminantes (§2.2 do brief). */
    public record Resultado(Tutor tutor, ContaTutor conta, InviteInfo convite) {}
}
