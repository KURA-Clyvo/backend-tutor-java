package br.com.clyvo.kura.tutor.consentimento.application;

import br.com.clyvo.kura.tutor.consentimento.domain.Consentimento;
import br.com.clyvo.kura.tutor.consentimento.domain.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioLgpdService {

    private final TutorRepository         tutorRepository;
    private final ConsentimentoRepository consentimentoRepository;
    private final ContaTutorRepository    contaTutorRepository;

    public RelatorioLgpdService(TutorRepository tutorRepository,
                                 ConsentimentoRepository consentimentoRepository,
                                 ContaTutorRepository contaTutorRepository) {
        this.tutorRepository         = tutorRepository;
        this.consentimentoRepository = consentimentoRepository;
        this.contaTutorRepository    = contaTutorRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gerarRelatorio(Long idTutor, String emailAutenticado) {
        Long idTutorAutenticado = contaTutorRepository.findIdTutorByEmail(emailAutenticado)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada para o e-mail autenticado."));
        if (!idTutorAutenticado.equals(idTutor)) {
            throw new ForbiddenException("Acesso negado: você só pode acessar seus próprios dados LGPD.");
        }
        Tutor tutor = tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));

        List<Consentimento> consentimentos =
                consentimentoRepository.findByTutor_IdTutorOrderByDtAceiteDesc(idTutor);

        Map<String, Object> relatorio = new LinkedHashMap<>();
        relatorio.put("geradoEm",  LocalDateTime.now().toString());
        relatorio.put("versaoLei", "LGPD - Lei 13.709/2018");
        relatorio.put("baseArt",   "Art. 18, I (direito de acesso) e V (portabilidade)");

        Map<String, Object> identificacao = new LinkedHashMap<>();
        identificacao.put("idTutor",        tutor.getIdTutor());
        identificacao.put("nome",           tutor.getNmTutor());
        identificacao.put("cpf",            mascararCpf(tutor.getNrCpf()));
        identificacao.put("email",          tutor.getDsEmail());
        identificacao.put("telefone",       tutor.getNrTelefone());
        identificacao.put("whatsapp",       tutor.getDsWhatsapp());
        identificacao.put("dataNascimento", tutor.getDtNascimento());
        identificacao.put("cidade",         tutor.getNmCidade());
        identificacao.put("uf",             tutor.getSgUf());
        identificacao.put("cadastradoEm",   tutor.getDtCriacao());
        relatorio.put("dadosPessoais", identificacao);

        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("recebeu",          tutor.temAvisoPrivacidade());
        aviso.put("dataRecebimento",  tutor.getDtAvisoPrivacidade());
        aviso.put("versaoAviso",      tutor.getDsVersaoAviso());
        relatorio.put("avisoPrivacidade", aviso);

        List<Map<String, Object>> listaConsentimentos = consentimentos.stream()
                .map(c -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("tipo",          c.getDsTipo());
                    item.put("versaoTermo",   c.getDsVersaoTermo());
                    item.put("aceito",        c.isAceito());
                    item.put("ativo",         c.isAtivo());
                    item.put("dataAceite",    c.getDtAceite());
                    item.put("ipAceite",      mascararIp(c.getDsIpAceite()));
                    item.put("dataRevogacao", c.getDtRevogacao());
                    return item;
                }).toList();
        relatorio.put("historicoConsentimentos", listaConsentimentos);

        relatorio.put("basesLegais", Map.of(
            "dadosCadastrais", "Art. 7º, V — execução de contrato com a clínica",
            "prontuarioPet",   "Art. 7º, VI — obrigação legal (CFMV)",
            "comunicacoes",    "Art. 7º, I — consentimento (quando aceito em CONSENTIMENTO)",
            "ipConsentimento", "Art. 7º, II — cumprimento de obrigação legal (evidência ANPD)"
        ));

        return relatorio;
    }

    private String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 6) return "***";
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(cpf.length() - 2);
    }

    private String mascararIp(String ip) {
        if (ip == null) return null;
        String[] partes = ip.split("\\.");
        if (partes.length == 4) return partes[0] + "." + partes[1] + ".*.*";
        return "***";
    }
}
