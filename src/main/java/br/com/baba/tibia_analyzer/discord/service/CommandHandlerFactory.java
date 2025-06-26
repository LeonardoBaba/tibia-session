package br.com.baba.tibia_analyzer.discord.service;

import br.com.baba.tibia_analyzer.discord.enums.CommandEnum;
import br.com.baba.tibia_analyzer.discord.interactions.commands.CommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class CommandHandlerFactory {

    @Autowired
    private ApplicationContext applicationContext;

    public CommandHandler getHandler(CommandEnum commandEnum) {
        return applicationContext.getBean(commandEnum.getHandlerClass());
    }
}
