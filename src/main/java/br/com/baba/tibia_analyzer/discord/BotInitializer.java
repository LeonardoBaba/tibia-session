package br.com.baba.tibia_analyzer.discord;

import br.com.baba.tibia_analyzer.discord.listener.SlashCommandHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotInitializer {

    private final SlashCommandHandler slashCommandHandler;
    @Value("${discord.bot.token}")
    private String botToken;

    public BotInitializer(SlashCommandHandler slashCommandHandler) {
        this.slashCommandHandler = slashCommandHandler;
    }

    @Bean
    public JDA init() {
        JDA api = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(slashCommandHandler)
                .build();
        api.updateCommands()
                .addCommands(Commands.slash("loot", "Divides the loot based on the Party Hunt Analyzer"))
                .queue();
        return api;
    }

}
