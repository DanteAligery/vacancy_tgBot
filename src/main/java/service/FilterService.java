package com.vacancybot.service;

import com.vacancybot.model.Vacancy;
import com.vacancybot.model.UserFilter;

import java.util.List;
import java.util.stream.Collectors;

public class FilterService {
    
    /**
     * Применяет фильтры пользователя к списку вакансий
     */
    public List<Vacancy> applyFilters(List<Vacancy> vacancies, UserFilter filter) {
        if (filter == null || !filter.isActive()) {
            return vacancies;
        }
        
        return vacancies.stream()
                .filter(v -> matchesKeywords(v, filter.getKeywords()))
                .filter(v -> v.matchesSalary(filter.getMinSalary(), filter.getMaxSalary()))
                .filter(v -> v.matchesCity(filter.getCity()))
                .filter(v -> v.matchesRemote(filter.isRemoteOnly()))
                .filter(v -> v.matchesExperience(filter.getMinExperienceYears()))
                .filter(v -> !filter.isExcludeAgencies() || !v.isAgency())
                .collect(Collectors.toList());
    }
    
    /**
     * Проверка соответствия ключевым словам
     */
    private boolean matchesKeywords(Vacancy vacancy, java.util.Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        
        String title = vacancy.getTitle() != null ? vacancy.getTitle().toLowerCase() : "";
        String company = vacancy.getCompany() != null ? vacancy.getCompany().toLowerCase() : "";
        String description = title + " " + company;
        
        for (String keyword : keywords) {
            if (description.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Сортировка вакансий по дате (новые сверху)
     */
    public List<Vacancy> sortByDate(List<Vacancy> vacancies) {
        return vacancies.stream()
                .sorted((v1, v2) -> {
                    if (v1.getPublishedAt() == null) return 1;
                    if (v2.getPublishedAt() == null) return -1;
                    return v2.getPublishedAt().compareTo(v1.getPublishedAt());
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Сортировка по зарплате (высокие сверху)
     */
    public List<Vacancy> sortBySalary(List<Vacancy> vacancies) {
        return vacancies.stream()
                .sorted((v1, v2) -> {
                    Integer s1 = v1.getSalaryMax() != null ? v1.getSalaryMax() : 0;
                    Integer s2 = v2.getSalaryMax() != null ? v2.getSalaryMax() : 0;
                    return s2.compareTo(s1);
                })
                .collect(Collectors.toList());
    }
}