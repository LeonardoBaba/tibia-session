package br.com.baba.tibia_analyzer.discord.interactions.modals;

import br.com.baba.tibia_analyzer.core.service.PartyHuntService;
import br.com.baba.tibia_analyzer.core.service.PartyHuntService.ProcessedSession;
import br.com.baba.tibia_analyzer.core.service.UserService;
import br.com.baba.tibia_analyzer.discord.embed.PartyHuntEmbedFactory;
import br.com.baba.tibia_analyzer.discord.enums.InputEnum;
import net.dv8tion.jda.api.entities.User;
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

    @Autowired
    private UserService userService;

    @Override
    public void process(ModalInteractionEvent event) {
        event.deferReply().queue();

        String input = event.getValue(InputEnum.SESSION_INPUT.getId()).getAsString();
        User discordUser = event.getUser();
        String ownerDiscordId = discordUser.getId();

        // Garante que existe row em `users` (mesmo que o usuário nunca tenha
        // logado no site). Quando ele logar, o avatar/nome serão atualizados.
        userService.upsertFromDiscord(
                ownerDiscordId,
                discordUser.getEffectiveName(),
                discordUser.getEffectiveAvatarUrl());

        ProcessedSession processed = partyHuntService.processAndPersist(
                input, null, null, ownerDiscordId);

        FileUpload sessionFile = FileUpload.fromData(
                input.getBytes(StandardCharsets.UTF_8), SESSION_FILE_NAME);

        event.getHook()
                .sendMessageEmbeds(partyHuntEmbedFactory.build(
                        processed.splitterResult(), processed.saved().getId()))
                .addFiles(sessionFile)
                .queue();
    }
}
