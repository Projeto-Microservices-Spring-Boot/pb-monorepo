//package com.edu.infnet.pb.stickers.Service;
//
//public List<TrocaDTO> BuscarMatches(String token) {
//
//    PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);
//
//    UUID meuId = usuario.id();
//
//    List<Object[]> elesTemQueEuPreciso = figurinhaUsuarioRepository.contarOQueEleTemQueEuPreciso(meuId);
//    List<Object[]> euTenhoQueElesPrecisam = figurinhaUsuarioRepository.contarOQueEuTenhoQueEleNecessita(meuId);
//    List<Object[]> resultado = figurinhaUsuarioRepository.buscarFigurinhasQueEleTemQueEuPreciso(meuId);
//
//    Map<UUID , Integer> mapaRecebo = new HashMap<>();
//    for (Object[] linha : elesTemQueEuPreciso) {
//        mapaRecebo.put((UUID) linha[0] , ((Long) linha[1]).intValue()); // aqui esta dizendo que linha[0] e o usuario e a linha[1] e a quantidade de cartas que eu preciso
//    }
//
//    Map<UUID, Integer> mapaOfereco = new HashMap<>();
//    for (Object[] linha : euTenhoQueElesPrecisam) {
//        mapaOfereco.put((UUID) linha[0], ((Long) linha[1]).intValue());
//    }
//
//
//    Map<UUID , List<FigurinhaTrocaDTO>> mapaFigurinhas = new HashMap<>();
//    for (Object[] linha : resultado){
//        UUID usuarioId = (UUID) linha[0];
//
//        Long figurinhaId = (Long) linha[1];
//
//        String jogador = (String) linha[2];
//
//        mapaFigurinhas.computeIfAbsent(usuarioId , k -> new ArrayList<>()).add( new FigurinhaTrocaDTO(figurinhaId , jogador));
//    }
//
//    return mapaRecebo.keySet().stream()
//            .filter(mapaOfereco::containsKey)
//            .map(outroUsuarioId -> {
//                int quantoEleTem = mapaRecebo.get(outroUsuarioId);
//                int quantoEuTenho = mapaOfereco.get(outroUsuarioId);
//                return new TrocaDTO(outroUsuarioId , quantoEleTem + quantoEuTenho ,quantoEuTenho , quantoEleTem , mapaFigurinhas.getOrDefault(outroUsuarioId , Collections.emptyList()));
//            })
//            .sorted(Comparator.comparing(TrocaDTO::score).reversed())
//            .toList();
//}
//
//public List<TrocaDTO> filtrarMatchesPorUsuarios(List<TrocaDTO> matches, List<UUID> usuariosPermitidos) {
//    Set<UUID> idsPermitidos = new HashSet<>(usuariosPermitidos);
//    return matches.stream()
//            .filter(troca -> idsPermitidos.contains(troca.usuarioId()))
//            .toList();
//}
