package br.com.baba.tibia_analyzer.discord.enums;

import lombok.Getter;

@Getter
public enum InputEnum {

    SESSION_INPUT("sessionInput");

    private String id;

    InputEnum(String id) {
        this.id = id;
    }

}
