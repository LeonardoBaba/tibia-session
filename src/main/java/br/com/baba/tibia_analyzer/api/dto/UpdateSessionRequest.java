package br.com.baba.tibia_analyzer.api.dto;

public record UpdateSessionRequest(
        String name,
        String comment
) {}
