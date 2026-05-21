package br.com.clyvo.kura.tutor.auth.security;

import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega ContaTutor para o Spring Security usando idConta como chave.
 *
 * O parâmetro {@code username} aqui é o subject do JWT — o idConta (Long como String).
 * Busca por ID é mais eficiente que por email e elimina o JOIN com TUTOR.
 *
 * Garante que contas bloqueadas ou inativas sejam rejeitadas pelo Spring Security
 * antes de chegar ao controller (accountLocked / disabled).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final ContaTutorRepository contaTutorRepository;

    public UserDetailsServiceImpl(ContaTutorRepository contaTutorRepository) {
        this.contaTutorRepository = contaTutorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String idContaStr) throws UsernameNotFoundException {
        Long idConta;
        try {
            idConta = Long.parseLong(idContaStr);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Subject do token não é um ID válido: " + idContaStr);
        }

        ContaTutor conta = contaTutorRepository.findById(idConta)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Conta não encontrada para o ID: " + idConta));

        return User.builder()
                .username(conta.getDsEmailLogin())
                .password(conta.getDsSenhaHash())
                .roles("TUTOR")
                .accountLocked(conta.isBloqueada())
                .disabled(!conta.isAtiva())
                .build();
    }
}
