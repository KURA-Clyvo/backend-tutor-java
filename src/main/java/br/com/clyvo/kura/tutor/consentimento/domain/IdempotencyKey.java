package br.com.clyvo.kura.tutor.consentimento.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "IDEMPOTENCY_KEY")
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_IDEMPOTENCY")
    private Long idIdempotency;

    @Column(name = "DS_KEY", nullable = false, length = 64)
    private String dsKey;

    @Column(name = "NM_RESOURCE", nullable = false, length = 60)
    private String nmResource;

    @Column(name = "ID_RESOURCE_CRIADO", nullable = false)
    private Long idResourceCriado;

    @Column(name = "DT_CRIACAO", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "DT_EXPIRACAO", nullable = false)
    private LocalDateTime dtExpiracao;

    protected IdempotencyKey() {}

    public static IdempotencyKey criar(String dsKey, String nmResource, Long idResourceCriado) {
        IdempotencyKey k = new IdempotencyKey();
        k.dsKey            = dsKey;
        k.nmResource       = nmResource;
        k.idResourceCriado = idResourceCriado;
        k.dtCriacao        = LocalDateTime.now();
        k.dtExpiracao      = k.dtCriacao.plusHours(24);
        return k;
    }

    public boolean isValido() { return dtExpiracao.isAfter(LocalDateTime.now()); }

    public Long getIdIdempotency()    { return idIdempotency; }
    public String getDsKey()          { return dsKey; }
    public String getNmResource()     { return nmResource; }
    public Long getIdResourceCriado() { return idResourceCriado; }
    public LocalDateTime getDtCriacao()   { return dtCriacao; }
    public LocalDateTime getDtExpiracao() { return dtExpiracao; }
}
