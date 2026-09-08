package br.com.clyvo.kura.tutor.auth.application;

import br.com.clyvo.kura.tutor.auth.api.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SJ3-11 — prova de INTEGRAÇÃO (não Mockito) de que {@code nr_tentativas_login}
 * PERSISTE quando {@code AuthService.login()} lança {@link BadCredentialsException}
 * por senha errada.
 *
 * <p><b>Por que não Mockito:</b> {@code AuthServiceTest} (Mockito puro) fica VERDE sob o
 * bug — {@code verify(contaRepo.save(...))} prova que o método foi CHAMADO, nunca que a
 * transação COMMITOU. Sob rollback do Spring, o objeto Java em memória continua com o
 * contador incrementado (é o MESMO objeto), então até uma assertion em memória mentiria.
 *
 * <p><b>Por que o método de teste NÃO é {@code @Transactional}:</b> se fosse, o
 * {@code @Transactional} de {@code login()} se juntaria à transação do teste (mesmo
 * {@code PlatformTransactionManager}) e o rollback que este teste tenta observar mudaria
 * de semântica — o teste ficaria verde medindo outra coisa (CLAUDE.md regra 13: teste que
 * passa no setup errado é indistinguível de teste que funciona).
 *
 * <p>A leitura de verificação usa {@link JdbcTemplate} (SQL cru) chamado a partir do
 * método de teste (que não é transacional) — portanto roda em autocommit, FORA de
 * qualquer transação Spring, e depois que {@code login()} já retornou/lançou (a proxy de
 * {@code @Transactional} só devolve o controle ao chamador depois de decidir
 * commit/rollback). Não há caminho para essa leitura enxergar um estado não commitado por
 * engano.
 */
@SpringBootTest
class AuthServiceLoginPersistenceTest {

    @Autowired AuthService     authService;
    @Autowired JdbcTemplate    jdbc;
    @Autowired PasswordEncoder encoder;

    private static final long   ID_CLINICA_SEED = 1L; // seedada por afterMigrate__seeds_dev.sql
    private static final String SENHA_CORRETA   = "Senha@Sj311";

    @Test
    @DisplayName("SJ3-11 — 1 senha errada tem que persistir nr_tentativas_login=1, "
            + "lido numa leitura NOVA fora da transação de login()")
    void senhaErradaDevePersistirIncrementoDoContador() {
        String sufixo = "sj311" + System.nanoTime();
        String email  = sufixo + "@teste.kura";
        long idTutor  = plantarTutor(sufixo, email);
        plantarConta(idTutor, email, encoder.encode(SENHA_CORRETA));

        LoginRequest requestSenhaErrada = new LoginRequest(email, "SenhaErrada@000");

        assertThatThrownBy(() -> authService.login(requestSenhaErrada))
                .isInstanceOf(BadCredentialsException.class);

        int tentativasApos = lerTentativas(email);

        assertThat(tentativasApos)
                .as("o incremento de nr_tentativas_login (registrarLoginFalha() + save(), "
                        + "AuthService.java:73-74) tem que sobreviver ao throw de "
                        + "BadCredentialsException lançado logo depois (:75), dentro da mesma "
                        + "transação de login(). Se este número vier 0, o rollback padrão do "
                        + "Spring esta desfazendo o save() -- a proteção anti-brute-force esta "
                        + "inerte.")
                .isEqualTo(1);
    }

    // ─── plantação direta via JDBC ──────────────────────────────────────────────
    // Tutor é @Immutable e não tem construtor público/setters (owned pelo .NET,
    // Java só lê) — não dá para persistir via JPA/TutorRepository em teste. INSERT
    // cru é o único jeito de plantar um Tutor + ContaTutor próprios, isolados por
    // um e-mail/CPF únicos (System.nanoTime()), sem depender do tutor id=1 seedado
    // (que outros testes desta suíte também podem usar).

    private long plantarTutor(String sufixo, String email) {
        String cpf = cpfUnico();
        jdbc.update("INSERT INTO TUTOR (ID_CLINICA, NM_TUTOR, NR_CPF, DS_EMAIL, DS_TELEFONE, "
                        + "ST_ATIVO) VALUES (?, ?, ?, ?, ?, 'S')",
                ID_CLINICA_SEED, "Tutor SJ3-11 " + sufixo, cpf, email, "11999990002");
        Long id = jdbc.queryForObject("SELECT ID_TUTOR FROM TUTOR WHERE DS_EMAIL = ?", Long.class, email);
        if (id == null) {
            throw new IllegalStateException("Falha ao plantar TUTOR de teste para " + email);
        }
        return id;
    }

    private void plantarConta(long idTutor, String emailLogin, String senhaHash) {
        jdbc.update("INSERT INTO CONTA_TUTOR (ID_TUTOR, DS_EMAIL_LOGIN, DS_SENHA_HASH, "
                        + "NR_TENTATIVAS_LOGIN, ST_ATIVA, ST_EMAIL_VERIFICADO) "
                        + "VALUES (?, ?, ?, 0, 'S', 'N')",
                idTutor, emailLogin, senhaHash);
    }

    private int lerTentativas(String emailLogin) {
        Integer valor = jdbc.queryForObject(
                "SELECT NR_TENTATIVAS_LOGIN FROM CONTA_TUTOR WHERE DS_EMAIL_LOGIN = ?",
                Integer.class, emailLogin);
        return valor == null ? -1 : valor;
    }

    private String cpfUnico() {
        // 11 dígitos, últimos 11 dígitos de nanoTime — CPF real não é validado no
        // INSERT cru (só a UNIQUE constraint de 11 chars), e cada chamada tem seu
        // próprio nanoTime.
        String n = Long.toString(System.nanoTime());
        return n.length() > 11 ? n.substring(n.length() - 11) : String.format("%011d", Long.parseLong(n));
    }
}
