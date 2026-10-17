// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.application;

import br.com.clyvo.kura.tutor.loja.bff.api.dto.CatalogoResponse;
import br.com.clyvo.kura.tutor.loja.bff.api.dto.ItemCatalogoResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoService {

    private final CatalogoRepositorio repositorio;

    public CatalogoService(CatalogoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public CatalogoResponse listar(CategoriaLoja categoria, int page, int size) {
        FatiaCatalogo fatia = repositorio.listarAtivos(categoria, page, size);
        List<ItemCatalogoResponse> itens = fatia.itens().stream()
                .map(ItemCatalogoResponse::de)
                .toList();
        return new CatalogoResponse(LojaAviso.TEXTO, itens, page, size, fatia.temProxima());
    }
}
