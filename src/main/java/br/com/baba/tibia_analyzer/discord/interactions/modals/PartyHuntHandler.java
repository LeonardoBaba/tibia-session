package br.com.baba.tibia_analyzer.discord.interactions.modals;

import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import br.com.baba.tibia_analyzer.core.service.PartyHuntService;
import br.com.baba.tibia_analyzer.discord.embed.PartyHuntEmbedFactory;
import br.com.baba.tibia_analyzer.discord.enums.InputEnum;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PartyHuntHandler implements ModalHandler {

    private static final String SESSION_FILE_NAME = "session.txt";

    @Autowired
    private PartyHuntService partyHuntService;

    @Autowired
    private PartyHuntEmbedFactory partyHuntEmbedFactory;

    @Override
    public void process(ModalInteractionEvent event) {
        event.deferReply().queue();

        String input = event.getValue(InputEnum.SESSION_INPUT.getId()).getAsString();
        String ownerDiscordId = event.getUser().getId();
        SessionResultDTO result = partyHuntService.processSession(input, null, null, ownerDiscordId);

        FileUpload sessionFile = FileUpload.fromData(
                input.getBytes(StandardCharsets.UTF_8), SESSION_FILE_NAME);

        event.getHook()
                .sendMessageEmbeds(partyHuntEmbedFactory.build(result))
                .addFiles(sessionFile)
                .queue();
    }
}
