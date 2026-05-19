package br.com.clyvo.kura.tutor.lgpd;

import br.com.clyvo.kura.tutor.entity.Consentimento;
import br.com.clyvo.kura.tutor.entity.Tutor;
import br.com.clyvo.kura.tutor.exception.RecursoNaoEncontradoException;
import br.com.clyvo.kura.tutor.repository.ConsentimentoRepository;
import br.com.clyvo.kura.tutor.repository.TutorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementa o direito de acesso e portabilidade de dados (LGPD art. 18, I e V).
 *
 * Gera um relatorio completo dos dados pessoais que o sistema armazena sobre o tutor.
 * Exigido pela ANPD quando o titular solicita saber quais dados existem sobre ele.
 *
 * Consumido pelo endpoint GET /tutores/{id}/lgpd/relatorio
 */
@Service
public class RelatorioLgpdService {

    private final TutorRepository tutorRepository;
    private final ConsentimentoRepository consentimentoRepository;

    public RelatorioLgpdService(TutorRepository tutorRepository,
                                 ConsentimentoRepository consentimentoRepository) {
        this.tutorRepository = tutorRepository;
        this.consentimentoRepository = consentimentoRepository;
    }

    /**
     * Gera mapa estruturado com todos os dados pessoais do tutor.
     * Retornado como JSON pelo controller — o tutor ve exatamente o que guardamos.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> gerarRelatorio(Long idTutor) {
        Tutor tutor = tutorRepository.findByIdTutorAndStAtivo(idTutor, "S")
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", idTutor));

        List<Consentimento> consentimentos =
                consentimentoRepository.findByTutor_IdTutorOrderByDtAceiteDesc(idTutor);

        Map<String, Object> relatorio = new LinkedHashMap<>();

        // Metadados do relatorio
        relatorio.put("geradoEm", LocalDateTime.now().toString());
        relatorio.put("versaoLei", "LGPD - Lei 13.709/2018");
        relatorio.put("baseArt", "Art. 18, I (direito de acesso) e V (portabilidade)");

        // Dados de identificacao
        Map<String, Object> identificacao = new LinkedHashMap<>();
        identificacao.put("idTutor", tutor.getIdTutor());
        identificacao.put("nome", tutor.getNmTutor());
        identificacao.put("cpf", mascararCpf(tutor.getNrCpf()));
        identificacao.put("email", tutor.getDsEmail());
        identificacao.put("telefone", tutor.getNrTelefone());
        identificacao.put("whatsapp", tutor.getDsWhatsapp());
        identificacao.put("dataNascimento", tutor.getDtNascimento());
        identificacao.put("cidade", tutor.getNmCidade());
        identificacao.put("uf", tutor.getSgUf());
        identificacao.put("cadastradoEm", tutor.getDtCriacao());
        relatorio.put("dadosPessoais", identificacao);

        // Aviso de privacidade (transparencia LGPD)
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("recebeu", tutor.temAvisoPrivacidade());
        aviso.put("dataRecebimento", tutor.getDtAvisoPrivacidade());
        aviso.put("versaoAviso", tutor.getDsVersaoAviso());
        relatorio.put("avisoPrivacidade", aviso);

        // Historico de consentimentos
        List<Map<String, Object>> listaConsentimentos = consentimentos.stream()
                .map(c -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("tipo", c.getDsTipo());
                    item.put("versaoTermo", c.getDsVersaoTermo());
                    item.put("aceito", c.isAceito());
                    item.put("ativo", c.isAtivo());
                    item.put("dataAceite", c.getDtAceite());
                    item.put("ipAceite", mascararIp(c.getDsIpAceite()));
                    item.put("dataRevogacao", c.getDtRevogacao());
                    return item;
                }).toList();
        relatorio.put("historicoConsentimentos", listaConsentimentos);

        // Bases legais aplicadas
        relatorio.put("basesLegais", Map.of(
            "dadosCadastrais", "Art. 7o, V — execucao de contrato com a clinica",
            "prontuarioPet",   "Art. 7o, VI — obrigacao legal (CFMV)",
            "comunicacoes",    "Art. 7o, I — consentimento (quando aceito em CONSENTIMENTO)",
            "ipConsentimento", "Art. 7o, II — cumprimento de obrigacao legal (evidencia ANPD)"
        ));

        return relatorio;
    }

    // Mascara CPF para exibicao: 123.456.789-00 → 123.***.***-00
    private String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 6) return "***";
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(cpf.length() - 2);
    }

    // Mascara IP para exibicao: 192.168.1.100 → 192.168.*.*
    private String mascararIp(String ip) {
        if (ip == null) return null;
        String[] partes = ip.split("\\.");
        if (partes.length == 4) return partes[0] + "." + partes[1] + ".*.*";
        return "***";
    }
}
