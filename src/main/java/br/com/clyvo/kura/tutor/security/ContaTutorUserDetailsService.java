package br.com.clyvo.kura.tutor.security;

import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementação de {@link UserDetailsService} que carrega a conta do tutor
 * a partir da tabela {@code CONTA_TUTOR} usando o e-mail como username.
 *
 * <p><strong>Por que construtor explícito?</strong>
 * O Eclipse/STS não processa {@code @RequiredArgsConstructor} do Lombok sem o
 * plugin instalado. Construtor explícito resolve os erros "field não inicializado"
 * sem precisar do plugin, e o Spring injeta normalmente.
 *
 * <p><strong>Sobre os getters do Lombok:</strong> getDsEmailLogin(), getDsSenhaHash(),
 * getDtBloqueio() e getStAtiva() são gerados pelo @Getter na entidade ContaTutor.
 * Se o Eclipse continuar reclamando desses métodos, instale o plugin Lombok:
 * Help → Eclipse Marketplace → pesquise "Lombok" → Install.
 */
@Service
public class ContaTutorUserDetailsService implements UserDetailsService {

    private final ContaTutorRepository contaTutorRepository;

    public ContaTutorUserDetailsService(ContaTutorRepository contaTutorRepository) {
        this.contaTutorRepository = contaTutorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var conta = contaTutorRepository.findByDsEmailLogin(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Conta não encontrada para o e-mail: " + email));

        return User.builder()
                .username(conta.getDsEmailLogin())
                .password(conta.getDsSenhaHash())
                .roles("TUTOR")
                .accountLocked(conta.getDtBloqueio() != null)
                .disabled(!"S".equals(conta.getStAtiva()))
                .build();
    }
}
