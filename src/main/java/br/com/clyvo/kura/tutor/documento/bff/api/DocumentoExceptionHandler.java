package br.com.clyvo.kura.tutor.documento.bff.api;

import br.com.clyvo.kura.tutor.documento.application.ArquivoGrandeException;
import br.com.clyvo.kura.tutor.documento.application.DocumentoInvalidoException;
import br.com.clyvo.kura.tutor.documento.application.LimiteDocumentosException;
import br.com.clyvo.kura.tutor.documento.application.TipoArquivoNaoSuportadoException;
import br.com.clyvo.kura.tutor.shared.exception.ApiError;
import br.com.clyvo.kura.tutor.shared.exception.MongoExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Handlers do pacote documento, escopados por {@code basePackages} para não mudar a resposta
 * de nenhum outro endpoint. O 503 de Mongo e o 400 de parâmetro vêm de {@link MongoExceptionHandlerBase}.
 */
@RestControllerAdvice(basePackages = "br.com.clyvo.kura.tutor.documento")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentoExceptionHandler extends MongoExceptionHandlerBase {

    @Override
    protected String modulo() {
        return "Documentos";
    }

    @Override
    protected Erro erroIndisponivel() {
        return new Erro("DOCUMENTOS_INDISPONIVEL",
                "Os documentos estão temporariamente indisponíveis. Tente novamente em instantes.");
    }

    @ExceptionHandler(LimiteDocumentosException.class)
    public ResponseEntity<ApiError> limite(LimiteDocumentosException ex, HttpServletRequest req) {
        return erro(409, "LIMITE_DOCUMENTOS", ex.getMessage(), req);
    }

    @ExceptionHandler(ArquivoGrandeException.class)
    public ResponseEntity<ApiError> grande(ArquivoGrandeException ex, HttpServletRequest req) {
        return erro(413, "ARQUIVO_MUITO_GRANDE", ex.getMessage(), req);
    }

    @ExceptionHandler(TipoArquivoNaoSuportadoException.class)
    public ResponseEntity<ApiError> tipo(TipoArquivoNaoSuportadoException ex, HttpServletRequest req) {
        return erro(415, "TIPO_ARQUIVO_NAO_SUPORTADO", ex.getMessage(), req);
    }

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ResponseEntity<ApiError> invalido(DocumentoInvalidoException ex, HttpServletRequest req) {
        return erro(400, "VALIDACAO_INVALIDA", ex.getMessage(), req);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> faltando(Exception ex, HttpServletRequest req) {
        return erro(400, "VALIDACAO_INVALIDA", "Campos obrigatórios ausentes: arquivo e tipo.", req);
    }

    /**
     * POST que nao e multipart. O controller NAO declara {@code consumes}: a rejeicao por Content-Type
     * no mapeamento nasce antes de existir handler method e um advice com {@code basePackages} nao e
     * consultado nesse momento (cairia no catch-all global, 500). Sem {@code consumes}, o
     * {@code @RequestPart} lanca MultipartException DENTRO do handler, onde este advice alcanca.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> naoMultipart(MultipartException ex, HttpServletRequest req) {
        String tipo = req.getContentType();
        if (tipo == null || !tipo.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/")) {
            return erro(415, "CONTENT_TYPE_NAO_SUPORTADO", "Envie o documento como multipart/form-data.", req);
        }
        log.warn("Documentos: multipart invalido ({})", ex.getClass().getSimpleName());
        return erro(400, "VALIDACAO_INVALIDA", "Envio multipart invalido.", req);
    }

    private static ResponseEntity<ApiError> erro(int status, String codigo, String mensagem, HttpServletRequest req) {
        return resposta(status, new Erro(codigo, mensagem), req);
    }
}
