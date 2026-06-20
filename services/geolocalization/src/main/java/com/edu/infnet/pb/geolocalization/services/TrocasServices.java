package com.infnet.geolocalizacao.services;



import com.infnet.geolocalizacao.dto.trocaEventos.CreateTradeDTO;
import com.infnet.geolocalizacao.dto.trocaEventos.PlacesResponseDTO;
import com.infnet.geolocalizacao.model.Enums.PointType;
import com.infnet.geolocalizacao.model.PointMap;
import com.infnet.geolocalizacao.repository.PointMapRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class TrocasServices {

private final ApiServices apiServices;
private final PointMapRepository pointMapRepository;


    public ResponseEntity<PointMap> criarTroca(CreateTradeDTO createTradeDTO) {
        PointMap pointMap = new PointMap();

        pointMap.setNome(createTradeDTO.nome());
        pointMap.setDescricao(createTradeDTO.descricao());
        pointMap.setLatitude(createTradeDTO.latitude());
        pointMap.setLongitude(createTradeDTO.longitude());
        pointMap.setEndereco(createTradeDTO.endereco());

        pointMap.setTipo(PointType.TROCA);

        if (createTradeDTO.latitude() == null ||
                createTradeDTO.longitude() == null) {
            List<PlacesResponseDTO> locais = apiServices.buscarLocais(createTradeDTO.endereco()).block();

            if (locais != null && !locais.isEmpty()){
                PlacesResponseDTO local = locais.get(0);

                pointMap.setLatitude(
                        Double.parseDouble(local.lat())
                );
                pointMap.setLongitude(
                        Double.parseDouble(local.lon())
                );
            }

        }


        PointMap salvo = pointMapRepository.save(pointMap);

        return ResponseEntity.ok(salvo);
    }



}
