package br.com.baba.tibia_analyzer.discord.interactions.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface CommandHandler {
    void process(SlashCommandInteractionEvent event);
}
