package br.com.baba.tibia_analyzer.discord.enums;

import br.com.baba.tibia_analyzer.discord.interactions.commands.CommandHandler;
import br.com.baba.tibia_analyzer.discord.interactions.commands.LootCommandHandler;
import br.com.baba.tibia_analyzer.discord.interactions.commands.LootDetailsCommandHandler;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public enum CommandEnum {

    LOOT("loot", "Divides the loot based on the Party Hunt Analyzer", LootCommandHandler.class),
    LOOT_DETAILS("loot-details", "Divides the loot and saves the session with a name and details", LootDetailsCommandHandler.class);

    private String name;
    private String description;
    private Class<? extends CommandHandler> handlerClass;

    CommandEnum(String name, String description, Class<? extends CommandHandler> handlerClass) {
        this.name = name;
        this.description = description;
        this.handlerClass = handlerClass;
    }

    public static List<SlashCommandData> getAllCommands() {
        return Arrays.stream(values())
                .map(CommandEnum::toCommand)
                .collect(Collectors.toList());
    }

    public static Optional<CommandEnum> fromName(String name) {
        return Arrays.stream(values())
                .filter(cmd -> cmd.getName().equals(name))
                .findFirst();
    }

    public SlashCommandData toCommand() {
        return Commands.slash(name, description);
    }

}
