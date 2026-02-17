package com.vacancybot.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vacancy {
    private String id;
    private String title;
    private String company;
    private String salary;
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;
    private String experience;
    private Integer experienceYears;
    private String city;
    private String url;
    private String source;
    private boolean remote;
    private boolean agency;
    private LocalDateTime publishedAt;
    
    // Вспомогательные методы для фильтрации
    
    public boolean matchesSalary(Integer min, Integer max) {
        if (min == null && max == null) return true;
        
        // Парсим зарплату, если еще не распарсили
        parseSalaryIfNeeded();
        
        if (min != null && salaryMin != null && salaryMin < min) return false;
        if (max != null && salaryMax != null && salaryMax > max) return false;
        
        return true;
    }
    
    public boolean matchesCity(String targetCity) {
        if (targetCity == null || targetCity.isEmpty()) return true;
        if (city == null || city.isEmpty()) return false;
        
        String cityLower = city.toLowerCase();
        String targetLower = targetCity.toLowerCase();
        
        return cityLower.contains(targetLower) || targetLower.contains(cityLower);
    }
    
    public boolean matchesRemote(boolean remoteOnly) {
        if (!remoteOnly) return true;
        return remote;
    }
    
    public boolean matchesExperience(Integer minYears) {
        if (minYears == null) return true;
        if (experienceYears == null) return false;
        return experienceYears >= minYears;
    }
    
    private void parseSalaryIfNeeded() {
        if (salaryMin != null && salaryMax != null) return;
        if (salary == null || salary.isEmpty()) return;
        
        try {
            // Парсим строку зарплаты формата "от 100 000 до 150 000 руб"
            String cleanSalary = salary.replaceAll("[^0-9\\-]", " ").trim();
            String[] parts = cleanSalary.split("\\s+");
            
            if (parts.length >= 2 && parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
                salaryMin = Integer.parseInt(parts[0]);
                salaryMax = Integer.parseInt(parts[1]);
            } else if (parts.length >= 1 && parts[0].matches("\\d+")) {
                if (salary.contains("от")) {
                    salaryMin = Integer.parseInt(parts[0]);
                } else if (salary.contains("до")) {
                    salaryMax = Integer.parseInt(parts[0]);
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
    }
    
    public String toTelegramMessage() {
        StringBuilder sb = new StringBuilder();
        
        // Эмодзи в зависимости от источника
        String sourceEmoji = switch (source) {
            case "hh" -> "🏢";
            case "habr" -> "📘";
            case "linkedin" -> "🔗";
            case "getmatch" -> "🤝";
            default -> "📌";
        };
        
        sb.append(sourceEmoji).append(" <b>").append(escapeHtml(title)).append("</b>\n");
        sb.append("🏢 <b>Компания:</b> ").append(escapeHtml(company != null ? company : "Не указана")).append("\n");
        
        if (agency) {
            sb.append("🤝 <b>Агентство:</b> Да\n");
        }
        
        sb.append("💰 <b>Зарплата:</b> ").append(escapeHtml(salary != null ? salary : "Не указана")).append("\n");
        sb.append("📊 <b>Опыт:</b> ").append(escapeHtml(experience != null ? experience : "Не указан")).append("\n");
        
        String location = city != null ? city : "Не указан";
        if (remote) {
            location += " (удаленно)";
        }
        sb.append("📍 <b>Город:</b> ").append(escapeHtml(location)).append("\n");
        
        sb.append("🔗 <b>Ссылка:</b> ").append(url).append("\n");
        sb.append("📅 <b>Источник:</b> ").append(getSourceName()).append("\n");
        
        return sb.toString();
    }
    
    private String getSourceName() {
        return switch (source) {
            case "hh" -> "HeadHunter";
            case "habr" -> "Хабр Карьера";
            case "linkedin" -> "LinkedIn";
            case "getmatch" -> "GetMatch";
            default -> source;
        };
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}