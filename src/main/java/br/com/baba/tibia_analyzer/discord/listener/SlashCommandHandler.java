package br.com.baba.tibia_analyzer.discord.listener;

import br.com.baba.tibia_analyzer.discord.exception.ConverterException;
import br.com.baba.tibia_analyzer.discord.service.PartyHuntService;
import jakarta.annotation.Nonnull;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandHandler extends ListenerAdapter {

    @Autowired
    private PartyHuntService partyHuntService;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "loot":
                TextInput body = TextInput.create("sessionInput", "Session Analyzer", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Your Session Analyzer goes here")
                        .setMinLength(150)
                        .setMaxLength(1500)
                        .build();

                Modal modal = Modal.create("partyHunt", "Party Hunt Session")
                        .addComponents(ActionRow.of(body))
                        .build();

                event.replyModal(modal).queue();
                break;
            default:
                event.reply("Slash command not mapped, try another one!").setEphemeral(true).queue();
                break;
        }
    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        try {
            switch (event.getModalId()) {
                case "partyHunt":
                    String input = event.getValue("sessionInput").getAsString();
                    event.reply(partyHuntService.processSession(input)).queue();
                    break;
            }
        } catch (ConverterException e) {
            event.reply("Error processing your request!\n" + e.getMessage()).setEphemeral(true).queue();
        }

    }
}
