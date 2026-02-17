package service;

import com.google.gson.*;
import com.vacancybot.model.Vacancy;
import com.vacancybot.model.UserFilter;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HhRuService {
    
    private static final String API_URL = "https://api.hh.ru/vacancies";
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();
    
    /**
     * Поиск вакансий на hh.ru с применением фильтров
     */
    public List<Vacancy> searchVacancies(UserFilter filter, int daysBack) {
        List<Vacancy> results = new ArrayList<>();
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            
            Instant fromDate = Instant.now().minusSeconds(daysBack * 24L * 3600);
            String dateFrom = DateTimeFormatter.ISO_INSTANT.format(fromDate);
            
            // Если у пользователя есть ключевые слова, ищем по каждому
            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
                for (String keyword : filter.getKeywords()) {
                    String url = buildSearchUrl(keyword, filter, dateFrom);
                    List<Vacancy> keywordResults = executeSearch(client, url, keyword);
                    results.addAll(keywordResults);
                    
                    // Небольшая задержка между запросами
                    Thread.sleep(300);
                }
            } else {
                // Поиск без ключевых слов (общий)
                String url = buildSearchUrl("", filter, dateFrom);
                List<Vacancy> generalResults = executeSearch(client, url, "");
                results.addAll(generalResults);
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка в HhRuService: " + e.getMessage());
        }
        
        return results;
    }
    
    private String buildSearchUrl(String keyword, UserFilter filter, String dateFrom) {
        StringBuilder url = new StringBuilder(API_URL + "?");
        
        // Добавляем ключевое слово
        if (keyword != null && !keyword.isEmpty()) {
            url.append("text=").append(encodeUrl(keyword)).append("&");
        }
        
        // Поиск в названии вакансии
        url.append("search_field=name&");
        
        // Сортировка по дате
        url.append("order_by=publication_time&");
        
        // Количество результатов
        url.append("per_page=50&");
        
        // Дата публикации
        url.append("date_from=").append(encodeUrl(dateFrom)).append("&");
        
        // Фильтр по зарплате (если указан)
        if (filter.getMinSalary() != null) {
            url.append("salary_from=").append(filter.getMinSalary()).append("&");
        }
        if (filter.getMaxSalary() != null) {
            url.append("salary_to=").append(filter.getMaxSalary()).append("&");
        }
        
        // Фильтр по городу
        if (filter.getCity() != null && !filter.getCity().isEmpty()) {
            url.append("area=").append(getAreaId(filter.getCity())).append("&");
        }
        
        // Удаленная работа
        if (filter.isRemoteOnly()) {
            url.append("schedule=remote&");
        }
        
        // Только с указанной зарплатой
        url.append("only_with_salary=true");
        
        return url.toString();
    }
    
    private List<Vacancy> executeSearch(CloseableHttpClient client, String url, String keyword) {
        List<Vacancy> results = new ArrayList<>();
        
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "VacancyBot/3.0");
            
            try (var response = client.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonObject json = jsonParser.parse(responseBody).getAsJsonObject();
                
                if (json.has("items")) {
                    JsonArray items = json.getAsJsonArray("items");
                    
                    for (JsonElement item : items) {
                        Vacancy vacancy = parseVacancy(item.getAsJsonObject());
                        if (vacancy != null) {
                            results.add(vacancy);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при запросе к hh.ru: " + e.getMessage());
        }
        
        return results;
    }
    
    private Vacancy parseVacancy(JsonObject json) {
        try {
            String id = "hh_" + json.get("id").getAsString();
            String title = getJsonString(json, "name");
            String company = getNestedString(json, "employer", "name");
            
            // Проверяем, агентство ли это
            boolean isAgency = checkIfAgency(company, json);
            
            // Парсим зарплату
            String salary = parseSalary(json.getAsJsonObject("salary"));
            Integer salaryMin = extractSalaryMin(json.getAsJsonObject("salary"));
            Integer salaryMax = extractSalaryMax(json.getAsJsonObject("salary"));
            String salaryCurrency = extractSalaryCurrency(json.getAsJsonObject("salary"));
            
            // Опыт работы
            String experience = getNestedString(json, "experience", "name");
            Integer experienceYears = parseExperience(experience);
            
            // Город
            String city = getNestedString(json, "area", "name");
            
            // Удаленная работа
            boolean remote = checkIfRemote(json);
            
            // URL
            String url = "https://hh.ru/vacancy/" + json.get("id").getAsString();
            
            // Дата публикации
            LocalDateTime publishedAt = parseDate(getJsonString(json, "published_at"));
            
            return Vacancy.builder()
                    .id(id)
                    .title(title)
                    .company(company)
                    .salary(salary)
                    .salaryMin(salaryMin)
                    .salaryMax(salaryMax)
                    .salaryCurrency(salaryCurrency)
                    .experience(experience)
                    .experienceYears(experienceYears)
                    .city(city)
                    .url(url)
                    .source("hh")
                    .remote(remote)
                    .agency(isAgency)
                    .publishedAt(publishedAt)
                    .build();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean checkIfAgency(String company, JsonObject json) {
        if (company == null) return false;
        String lowerCompany = company.toLowerCase();
        
        // Проверяем по названию
        if (lowerCompany.contains("агентство") || 
            lowerCompany.contains("кадровое") || 
            lowerCompany.contains("recruitment") ||
            lowerCompany.contains("hr") ||
            lowerCompany.contains("персонал")) {
            return true;
        }
        
        // Проверяем тип работодателя
        JsonObject employer = json.getAsJsonObject("employer");
        if (employer != null && employer.has("type")) {
            String type = getJsonString(employer, "type");
            if ("agency".equals(type)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean checkIfRemote(JsonObject json) {
        // Проверяем расписание
        if (json.has("schedule")) {
            JsonObject schedule = json.getAsJsonObject("schedule");
            String scheduleName = getJsonString(schedule, "name");
            if (scheduleName != null && scheduleName.toLowerCase().contains("удален")) {
                return true;
            }
        }
        
        // Проверяем адрес
        if (json.has("address")) {
            JsonObject address = json.getAsJsonObject("address");
            if (address != null && address.has("raw")) {
                String raw = getJsonString(address, "raw");
                if (raw != null && raw.toLowerCase().contains("удален")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private Integer parseExperience(String experience) {
        if (experience == null) return null;
        
        String lower = experience.toLowerCase();
        if (lower.contains("без опыта")) return 0;
        if (lower.contains("1-3")) return 1;
        if (lower.contains("3-6")) return 3;
        if (lower.contains("более 6")) return 6;
        
        return null;
    }
    
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(dateStr.replace("+03:00", ""), 
                DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    private String parseSalary(JsonObject salaryJson) {
        if (salaryJson == null || salaryJson.isJsonNull()) {
            return "Не указана";
        }
        
        try {
            String currency = getJsonString(salaryJson, "currency");
            boolean hasFrom = salaryJson.has("from") && !salaryJson.get("from").isJsonNull();
            boolean hasTo = salaryJson.has("to") && !salaryJson.get("to").isJsonNull();
            
            if (hasFrom && hasTo) {
                return salaryJson.get("from").getAsInt() + " - " + 
                       salaryJson.get("to").getAsInt() + " " + currency;
            } else if (hasFrom) {
                return "от " + salaryJson.get("from").getAsInt() + " " + currency;
            } else if (hasTo) {
                return "до " + salaryJson.get("to").getAsInt() + " " + currency;
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
        
        return "Не указана";
    }
    
    private Integer extractSalaryMin(JsonObject salaryJson) {
        if (salaryJson == null || salaryJson.isJsonNull()) return null;
        try {
            if (salaryJson.has("from") && !salaryJson.get("from").isJsonNull()) {
                return salaryJson.get("from").getAsInt();
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return null;
    }
    
    private Integer extractSalaryMax(JsonObject salaryJson) {
        if (salaryJson == null || salaryJson.isJsonNull()) return null;
        try {
            if (salaryJson.has("to") && !salaryJson.get("to").isJsonNull()) {
                return salaryJson.get("to").getAsInt();
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return null;
    }
    
    private String extractSalaryCurrency(JsonObject salaryJson) {
        if (salaryJson == null || salaryJson.isJsonNull()) return "RUB";
        return getJsonString(salaryJson, "currency");
    }
    
    private int getAreaId(String city) {
        // Упрощенный маппинг городов
        return switch (city.toLowerCase()) {
            case "москва" -> 1;
            case "санкт-петербург", "питер", "спб" -> 2;
            case "екатеринбург" -> 3;
            case "новосибирск" -> 4;
            case "казань" -> 88;
            default -> 1; // По умолчанию Москва
        };
    }
    
    private String getJsonString(JsonObject obj, String key) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }
    
    private String getNestedString(JsonObject obj, String parentKey, String childKey) {
        if (obj != null && obj.has(parentKey) && !obj.get(parentKey).isJsonNull()) {
            JsonObject parent = obj.getAsJsonObject(parentKey);
            return getJsonString(parent, childKey);
        }
        return "";
    }
    
    private String encodeUrl(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}