package model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilter {
    private Long chatId;
    
    // Фильтр по зарплате
    private Integer minSalary;
    private Integer maxSalary;
    private String salaryCurrency; // RUB, USD, EUR
    
    // Фильтр по городу
    private String city;
    private boolean remoteOnly; // только удаленная работа
    
    // Ключевые слова для поиска
    @Builder.Default
    private Set<String> keywords = new HashSet<>();
    
    // Источники для поиска
    @Builder.Default
    private Set<String> sources = new HashSet<>(Set.of("hh", "habr", "linkedin", "getmatch"));
    
    // Дополнительные фильтры
    private Integer minExperienceYears; // минимальный опыт в годах
    private boolean excludeAgencies; // исключить кадровые агентства
    
    public boolean hasSalaryFilter() {
        return minSalary != null || maxSalary != null;
    }
    
    public boolean hasCityFilter() {
        return city != null && !city.isEmpty();
    }
    
    public boolean isActive() {
        return hasSalaryFilter() || hasCityFilter() || remoteOnly || !keywords.isEmpty();
    }
    
    public String getDescription() {
        StringBuilder desc = new StringBuilder("🔍 Текущие фильтры:\n");
        
        if (!keywords.isEmpty()) {
            desc.append("• Ключевые слова: ").append(String.join(", ", keywords)).append("\n");
        }
        
        if (minSalary != null || maxSalary != null) {
            desc.append("• Зарплата: ");
            if (minSalary != null) desc.append("от ").append(minSalary);
            if (minSalary != null && maxSalary != null) desc.append(" ");
            if (maxSalary != null) desc.append("до ").append(maxSalary);
            desc.append(" ").append(salaryCurrency != null ? salaryCurrency : "RUB").append("\n");
        }
        
        if (city != null && !city.isEmpty()) {
            desc.append("• Город: ").append(city).append("\n");
        }
        
        if (remoteOnly) {
            desc.append("• Только удаленная работа\n");
        }
        
        if (excludeAgencies) {
            desc.append("• Без кадровых агентств\n");
        }
        
        return desc.toString();
    }
}