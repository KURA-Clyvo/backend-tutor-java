package br.com.clyvo.kura.tutor.auth.domain.repository;

import br.com.clyvo.kura.tutor.entity.ContaTutor;
import jakarta.persistence.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Valida que V7 adicionou os campos de push token e que a entidade os mapeia corretamente.
 *
 * Teste de reflexão pura (sem Spring/H2) porque:
 * - V2/V3/V5 contêm PL/SQL Oracle incompatível com H2 — bug pré-existente.
 * - ddl-auto=create-drop registra entidades em duplicata no contexto @DataJpaTest.
 * A migration V7 em si (ALTER TABLE) é validada via Oracle FIAP em staging/prod.
 */
class ContaTutorPushTokenTest {

    @Test
    @DisplayName("V7 — ContaTutor possui campo dsPushToken com @Column correto")
    void deveConterCampoDsPushToken() throws NoSuchFieldException {
        Field campo = ContaTutor.class.getDeclaredField("dsPushToken");

        Column col = campo.getAnnotation(Column.class);
        assertThat(col).as("@Column deve existir em dsPushToken").isNotNull();
        assertThat(col.name()).isEqualToIgnoringCase("ds_push_token");
        assertThat(col.length()).as("Comprimento deve ser 512").isEqualTo(512);
        assertThat(campo.getType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("V7 — ContaTutor possui campo dsPlataformaPush com @Column correto")
    void deveConterCampoDsPlataformaPush() throws NoSuchFieldException {
        Field campo = ContaTutor.class.getDeclaredField("dsPlataformaPush");

        Column col = campo.getAnnotation(Column.class);
        assertThat(col).as("@Column deve existir em dsPlataformaPush").isNotNull();
        assertThat(col.name()).isEqualToIgnoringCase("ds_plataforma_push");
        assertThat(col.length()).as("Comprimento deve ser 10 (ios/android)").isEqualTo(10);
        assertThat(campo.getType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("V7 — campos são nullable (não quebram inserts existentes)")
    void camposSaoNullable() throws NoSuchFieldException {
        Field pushToken    = ContaTutor.class.getDeclaredField("dsPushToken");
        Field plataforma   = ContaTutor.class.getDeclaredField("dsPlataformaPush");

        Column colToken = pushToken.getAnnotation(Column.class);
        Column colPlat  = plataforma.getAnnotation(Column.class);

        // nullable=true é o default de @Column — verificar que não foi marcado nullable=false
        assertThat(colToken.nullable()).as("dsPushToken deve ser nullable").isTrue();
        assertThat(colPlat.nullable()).as("dsPlataformaPush deve ser nullable").isTrue();
    }

    @Test
    @DisplayName("V7 — getter/setter de push token funcionam corretamente")
    void getterSetterPushToken() {
        ContaTutor conta = new ContaTutor();
        assertThatNoException().isThrownBy(() -> {
            conta.setDsPushToken("ExponentPushToken[abc123]");
            conta.setDsPlataformaPush("android");
        });
        assertThat(conta.getDsPushToken()).isEqualTo("ExponentPushToken[abc123]");
        assertThat(conta.getDsPlataformaPush()).isEqualTo("android");
    }

    @Test
    @DisplayName("V7 — migration SQL existe com ALTER TABLE correto")
    void migrationV7Existe() throws Exception {
        var url = ContaTutorPushTokenTest.class.getClassLoader()
                .getResource("db/migration/V7__conta_tutor_push_token.sql");
        assertThat(url).as("Arquivo V7 deve existir em db/migration/").isNotNull();

        String sql = new String(url.openStream().readAllBytes());
        assertThat(sql).as("V7 deve adicionar DS_PUSH_TOKEN")
                .containsIgnoringCase("DS_PUSH_TOKEN");
        assertThat(sql).as("V7 deve adicionar DS_PLATAFORMA_PUSH")
                .containsIgnoringCase("DS_PLATAFORMA_PUSH");
        assertThat(sql).as("V7 deve ser ALTER TABLE (não DROP/CREATE)")
                .containsIgnoringCase("ALTER TABLE CONTA_TUTOR");
    }
}
