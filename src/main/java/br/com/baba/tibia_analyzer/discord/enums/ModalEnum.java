package br.com.baba.tibia_analyzer.discord.enums;

import br.com.baba.tibia_analyzer.discord.interactions.modals.ModalHandler;
import br.com.baba.tibia_analyzer.discord.interactions.modals.PartyHuntHandler;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ModalEnum {

    SESSION("partyHunt", "Party Hunt Session", PartyHuntHandler.class);

    private String name;
    private String title;
    private Class<? extends ModalHandler> handlerClass;

    ModalEnum(String name, String title, Class<? extends ModalHandler> handlerClass) {
        this.name = name;
        this.title = title;
        this.handlerClass = handlerClass;
    }

    public static Optional<ModalEnum> fromName(String name) {
        return Arrays.stream(values())
                .filter(cmd -> cmd.getName().equals(name))
                .findFirst();
    }

}
