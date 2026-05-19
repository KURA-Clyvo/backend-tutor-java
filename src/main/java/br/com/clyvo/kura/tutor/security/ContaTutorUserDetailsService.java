package br.com.clyvo.kura.tutor.security;

import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.repository.ContaTutorRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega os dados do tutor para o Spring Security.
 *
 * Equivalente ao UserDetailsService definido em UsuarioConfig.java
 * do projeto de aula, mas usando ContaTutor (CONTA_TUTOR) em vez de Usuario.
 *
 * Aula buscava por RM: repU.findByRm(rm)
 * KURA busca por e-mail: contaTutorRepository.findByDsEmailLogin(email)
 */
@Service
public class ContaTutorUserDetailsService implements UserDetailsService {

    private final ContaTutorRepository contaTutorRepository;

    public ContaTutorUserDetailsService(ContaTutorRepository contaTutorRepository) {
        this.contaTutorRepository = contaTutorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        ContaTutor conta = contaTutorRepository.findByDsEmailLogin(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Conta nao encontrada para o e-mail: " + email));

        return User.builder()
                .username(conta.getDsEmailLogin())
                .password(conta.getDsSenhaHash())
                .roles("TUTOR")
                .accountLocked(conta.isBloqueada())
                .disabled(!conta.isAtiva())
                .build();
    }
}
