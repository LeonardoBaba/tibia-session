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
public class LootDetailsCommandHandler implements CommandHandler {
    @Override
    public void process(SlashCommandInteractionEvent event) {
        TextInput body = TextInput.create(InputEnum.SESSION_INPUT.getId(), "Session Analyzer", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Your Session Analyzer goes here")
                .setMinLength(150)
                .setMaxLength(4000)
                .build();

        TextInput name = TextInput.create(InputEnum.NAME_INPUT.getId(), "Session Name", TextInputStyle.SHORT)
                .setPlaceholder("Give this session a name")
                .setMaxLength(100)
                .setRequired(false)
                .build();

        TextInput details = TextInput.create(InputEnum.COMMENT_INPUT.getId(), "Details", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Add any details about this session")
                .setMaxLength(1000)
                .setRequired(false)
                .build();

        Modal modal = Modal.create(ModalEnum.SESSION.getName(), ModalEnum.SESSION.getTitle())
                .addComponents(ActionRow.of(body), ActionRow.of(name), ActionRow.of(details))
                .build();

        event.replyModal(modal).queue();
    }
}
