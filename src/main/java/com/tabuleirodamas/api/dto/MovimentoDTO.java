package com.tabuleirodamas.api.dto;

import java.util.List;

public record MovimentoDTO(List<String> caminho, List<String> capturas, boolean promove) {
}
