package br.com.baba.tibia_analyzer.discord.service;

import br.com.baba.tibia_analyzer.discord.enums.ModalEnum;
import br.com.baba.tibia_analyzer.discord.interactions.modals.ModalHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ModalHandlerFactory {

    @Autowired
    private ApplicationContext applicationContext;

    public ModalHandler getHandler(ModalEnum modalEnum) {
        return applicationContext.getBean(modalEnum.getHandlerClass());
    }
}

