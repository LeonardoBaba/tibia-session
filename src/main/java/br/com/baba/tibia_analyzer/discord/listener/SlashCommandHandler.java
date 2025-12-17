package br.com.baba.tibia_analyzer.discord.listener;

import br.com.baba.tibia_analyzer.discord.enums.CommandEnum;
import br.com.baba.tibia_analyzer.discord.enums.ModalEnum;
import br.com.baba.tibia_analyzer.core.exception.ConverterException;
import br.com.baba.tibia_analyzer.discord.interactions.commands.CommandHandler;
import br.com.baba.tibia_analyzer.discord.interactions.modals.ModalHandler;
import br.com.baba.tibia_analyzer.discord.service.CommandHandlerFactory;
import br.com.baba.tibia_analyzer.discord.service.ModalHandlerFactory;
import jakarta.annotation.Nonnull;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandHandler extends ListenerAdapter {

    @Autowired
    private ModalHandlerFactory modalHandlerFactory;

    @Autowired
    private CommandHandlerFactory commandHandlerFactory;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        CommandEnum.fromName(event.getName())
                .ifPresentOrElse(
                        cmd -> {
                            try {
                                CommandHandler handler = commandHandlerFactory.getHandler(cmd);
                                handler.process(event);
                            } catch (Exception e) {
                                event.reply("An error occurred while processing the command.").setEphemeral(true).queue();
                                System.out.println(e.getMessage());
                            }
                        },
                        () -> event.reply("Slash command not mapped, try another one!").setEphemeral(true).queue()
                );
    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        try {
            ModalEnum.fromName(event.getModalId())
                    .ifPresent(
                            modalEnum -> {
                                ModalHandler handler = modalHandlerFactory.getHandler(modalEnum);
                                handler.process(event);
                            }
                    );
        } catch (ConverterException e) {
            event.reply("Error processing your request!\n" + e.getMessage()).setEphemeral(true).queue();
        }

    }
}
