package br.com.baba.tibia_analyzer.discord.interactions.commands;

import br.com.baba.tibia_analyzer.discord.enums.InputEnum;
import br.com.baba.tibia_analyzer.discord.enums.ModalEnum;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.stereotype.Component;

@Component
public class LootCommandHandler implements CommandHandler {
    @Override
    public void process(SlashCommandInteractionEvent event) {
        TextInput body = TextInput.create(InputEnum.SESSION_INPUT.getId(), "Session Analyzer", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Your Session Analyzer goes here")
                .setMinLength(150)
                .setMaxLength(1500)
                .build();

        Modal modal = Modal.create(ModalEnum.SESSION.getName(), ModalEnum.SESSION.getTitle())
                .addComponents(ActionRow.of(body))
                .build();

        event.replyModal(modal).queue();
    }
}
