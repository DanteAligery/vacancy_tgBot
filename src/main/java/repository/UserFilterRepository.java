package com.vacancybot.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vacancybot.model.UserFilter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserFilterRepository {
    
    private static final String STORAGE_FILE = "user_filters.json";
    private final Map<Long, UserFilter> filters = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    public UserFilterRepository() {
        loadFromFile();
        // Добавляем фильтры по умолчанию
        addDefaultKeywords();
    }
    
    private void addDefaultKeywords() {
        String[] defaultKeywords = {
            "менеджер продукта",
            "product manager",
            "IT project manager",
            "team lead",
            "руководитель разработки",
            "head of",
            "cto",
            "технический директор",
            "product owner"
        };
        
        // Эти ключевые слова будут использоваться, если у пользователя нет своих
    }
    
    public UserFilter getFilter(Long chatId) {
        return filters.computeIfAbsent(chatId, id -> {
            UserFilter defaultFilter = UserFilter.builder()
                    .chatId(id)
                    .minSalary(null)
                    .maxSalary(null)
                    .remoteOnly(false)
                    .excludeAgencies(false)
                    .salaryCurrency("RUB")
                    .build();
            
            // Добавляем ключевые слова по умолчанию
            defaultFilter.getKeywords().add("менеджер");
            defaultFilter.getKeywords().add("product manager");
            defaultFilter.getKeywords().add("team lead");
            
            return defaultFilter;
        });
    }
    
    public void saveFilter(UserFilter filter) {
        filters.put(filter.getChatId(), filter);
        saveToFile();
    }
    
    public void updateMinSalary(Long chatId, Integer minSalary) {
        UserFilter filter = getFilter(chatId);
        filter.setMinSalary(minSalary);
        saveFilter(filter);
    }
    
    public void updateMaxSalary(Long chatId, Integer maxSalary) {
        UserFilter filter = getFilter(chatId);
        filter.setMaxSalary(maxSalary);
        saveFilter(filter);
    }
    
    public void updateCity(Long chatId, String city) {
        UserFilter filter = getFilter(chatId);
        filter.setCity(city);
        saveFilter(filter);
    }
    
    public void setRemoteOnly(Long chatId, boolean remoteOnly) {
        UserFilter filter = getFilter(chatId);
        filter.setRemoteOnly(remoteOnly);
        saveFilter(filter);
    }
    
    public void setExcludeAgencies(Long chatId, boolean exclude) {
        UserFilter filter = getFilter(chatId);
        filter.setExcludeAgencies(exclude);
        saveFilter(filter);
    }
    
    public void addKeyword(Long chatId, String keyword) {
        UserFilter filter = getFilter(chatId);
        filter.getKeywords().add(keyword);
        saveFilter(filter);
    }
    
    public void removeKeyword(Long chatId, String keyword) {
        UserFilter filter = getFilter(chatId);
        filter.getKeywords().remove(keyword);
        saveFilter(filter);
    }
    
    public void resetFilter(Long chatId) {
        filters.remove(chatId);
        getFilter(chatId); // Создаст новый с настройками по умолчанию
        saveToFile();
    }
    
    private void saveToFile() {
        try {
            Path path = Paths.get(STORAGE_FILE);
            String json = gson.toJson(filters);
            Files.writeString(path, json);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения фильтров: " + e.getMessage());
        }
    }
    
    private void loadFromFile() {
        try {
            Path path = Paths.get(STORAGE_FILE);
            if (Files.exists(path)) {
                String json = Files.readString(path);
                Type type = new TypeToken<Map<Long, UserFilter>>(){}.getType();
                Map<Long, UserFilter> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    filters.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки фильтров: " + e.getMessage());
        }
    }
}