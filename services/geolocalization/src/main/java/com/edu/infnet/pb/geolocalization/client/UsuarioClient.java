package com.edu.infnet.pb.geolocalization.client;

import com.edu.infnet.pb.geolocalization.Mock.UsuarioClientMock;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient( // FeignClient ela automaticamente cria uma implementação dessa interface para fazer as requisiçoes do http
// ou seja voce nao precisaria utilizar RestTemplate nem WebClient ou HttpURLConnection, a biblioteca em si faz tudo sozinho

        name = "usuario-service", // e apenas para identificar o cliente
        url = "http://localhost:8080", // aqui diz a base da url do api
        fallback = UsuarioClientMock.class // isso serve caso o api esteja fora do ar ele manda um mock ao inves de lançar um erro
)
public interface UsuarioClient {

    @GetMapping("/me")
    PerfilResponseDTO buscarUusarioLogado(
            @RequestHeader( "Authorization") String token
    );
}
