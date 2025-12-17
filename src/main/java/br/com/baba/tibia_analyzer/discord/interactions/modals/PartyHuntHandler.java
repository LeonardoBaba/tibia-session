package br.com.baba.tibia_analyzer.discord.interactions.modals;

import br.com.baba.tibia_analyzer.core.service.PartyHuntService;
import br.com.baba.tibia_analyzer.discord.enums.InputEnum;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PartyHuntHandler implements ModalHandler {

    @Autowired
    private PartyHuntService partyHuntService;

    @Override
    public void process(ModalInteractionEvent event) {
        event.deferReply().queue();
        
        String input = event.getValue(InputEnum.SESSION_INPUT.getId()).getAsString();

        event.getHook().sendMessage(partyHuntService.processSession(input)).queue();
    }
}
