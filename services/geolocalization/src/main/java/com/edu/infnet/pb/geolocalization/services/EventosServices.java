package com.infnet.geolocalizacao.services;



import com.infnet.geolocalizacao.dto.trocaEventos.CreateEventsDTO;
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
public class EventosServices {

private ApiServices apiServices;
private PointMapRepository pointMapRepository;


    public ResponseEntity<PointMap> criarEvento(CreateEventsDTO createEventsDTO) {
        PointMap pointMap = new PointMap();

        pointMap.setNome(createEventsDTO.nome());
        pointMap.setDescricao(createEventsDTO.descricao());
        pointMap.setEndereco(createEventsDTO.endereco());
        pointMap.setLatitude(createEventsDTO.latitude());
        pointMap.setLongitude(createEventsDTO.longitude());
        pointMap.setDataInicioEvento(createEventsDTO.dataInicio());
        pointMap.setDataFimEventos(createEventsDTO.dataFim());
        pointMap.setTipo(PointType.EVENTO);

        if (createEventsDTO.latitude() == null || createEventsDTO.longitude() == null) {
            List<PlacesResponseDTO> locais = apiServices.buscarLocais(createEventsDTO.endereco()).block();

            if(locais != null && !locais.isEmpty()) {

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

    public ResponseEntity<PointMap> deletandoEvento(Long id) {
    PointMap pointMap = pointMapRepository.findById(id).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));


    if(pointMap.getTipo() != PointType.EVENTO) {
        throw new RuntimeException("O ponto informado nao e um evento");
    }

    pointMapRepository.delete(pointMap);

    return ResponseEntity.noContent().build();
    }


    public List<PointMap> listarEventos() {
        return pointMapRepository.findAll();
    }


}
