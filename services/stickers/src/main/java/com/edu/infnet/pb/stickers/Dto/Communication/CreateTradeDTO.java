package com.edu.infnet.pb.stickers.Dto.Communication;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTradeDTO {
    private String nome;
    private String descricao;
    private String endereco;
    private Double latitude;
    private Double longitude;
}
