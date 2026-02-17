package com.mycompany.vacancy_tgbot;


import model.UserFilter;
import model.Vacancy;
import com.vacancybot.repository.UserFilterRepository;
import com.vacancybot.service.*;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Vacancy_tgbot implements LongPollingSingleThreadUpdateConsumer {
    
    private final TelegramClient telegramClient;
    private final UserFilterRepository filterRepository;
    private final VacancyAggregatorService aggregatorService;
    private final FilterService filterService;
    
    // Состояния для диалога с пользователем
    private enum DialogState {
        NONE,
        AWAITING_MIN_SALARY,
        AWAITING_MAX_SALARY,
        AWAITING_CITY,
        AWAITING_KEYWORD
    }
    
    private final java.util.Map<Long, DialogState> userStates = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Long, String> userTempData = new java.util.concurrent.ConcurrentHashMap<>();
    
    public VacancyBot(TelegramClient telegramClient, String habrToken) {
        this.telegramClient = telegramClient;
        this.filterRepository = new UserFilterRepository();
        this.aggregatorService = new VacancyAggregatorService(habrToken);
        this.filterService = new FilterService();
    }
    
    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String text = message.getText();
            
            try {
                handleMessage(chatId, text);
            } catch (Exception e) {
                sendMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
            }
        }
    }
    
    private void handleMessage(Long chatId, String text) {
        DialogState currentState = userStates.getOrDefault(chatId, DialogState.NONE);
        
       