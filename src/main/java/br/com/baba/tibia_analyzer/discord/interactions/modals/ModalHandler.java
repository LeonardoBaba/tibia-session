package br.com.baba.tibia_analyzer.discord.interactions.modals;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalHandler {
    void process(ModalInteractionEvent event);
}
