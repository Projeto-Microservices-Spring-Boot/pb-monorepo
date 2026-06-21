package com.edu.infnet.pb.stickers.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class AcceptTradeProposalDTO {
    private String meetingLocation;
    private LocalDateTime meetingAt;
}
