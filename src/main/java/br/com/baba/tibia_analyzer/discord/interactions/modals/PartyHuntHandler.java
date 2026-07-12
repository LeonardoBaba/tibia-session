package br.com.baba.tibia_analyzer.discord.interactions.modals;

import br.com.baba.tibia_analyzer.core.service.PartyHuntService;
import br.com.baba.tibia_analyzer.core.service.PartyHuntService.ProcessedSession;
import br.com.baba.tibia_analyzer.core.service.UserService;
import br.com.baba.tibia_analyzer.discord.embed.PartyHuntEmbedFactory;
import br.com.baba.tibia_analyzer.discord.enums.InputEnum;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
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

    @Autowired
    private UserService userService;

    @Override
    public void process(ModalInteractionEvent event) {
        event.deferReply().queue();

        String input = event.getValue(InputEnum.SESSION_INPUT.getId()).getAsString();
        String name = getOptionalValue(event, InputEnum.NAME_INPUT.getId());
        String comment = getOptionalValue(event, InputEnum.COMMENT_INPUT.getId());
        User discordUser = event.getUser();
        String ownerDiscordId = discordUser.getId();

        userService.upsertFromDiscord(
                ownerDiscordId,
                discordUser.getEffectiveName(),
                discordUser.getEffectiveAvatarUrl());

        ProcessedSession processed = partyHuntService.processAndPersist(
                input, name, comment, ownerDiscordId);

        FileUpload sessionFile = FileUpload.fromData(
                input.getBytes(StandardCharsets.UTF_8), SESSION_FILE_NAME);

        event.getHook()
                .sendMessageEmbeds(partyHuntEmbedFactory.build(
                        processed.splitterResult(), processed.saved().getId()))
                .addFiles(sessionFile)
                .queue();
    }

    private String getOptionalValue(ModalInteractionEvent event, String id) {
        ModalMapping mapping = event.getValue(id);
        if (mapping == null) {
            return null;
        }
        String value = mapping.getAsString();
        return value.isBlank() ? null : value;
    }
}
