package io.github.maksim0840.extractionresults.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Утилитный класс для преобразования JSON-структур (Map / List&lt;Map&gt;)
 * в строковые представления форматов JSON, XML и CSV.
 * <p>
 * JSON и XML выводятся с человекочитаемым форматированием (отступы, переносы строк).
 * CSV форматируется по схеме: первая строка — заголовки колонок, далее строки данных.
 * <p>
 * Все мапперы объявлены как {@code static final}: они дороги в создании,
 * но потокобезопасны после конфигурации.
 */
public class FormatsMapper {

    private static final ObjectMapper OM = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private static final XmlMapper XML_MAPPER = XmlMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
            .build();

    private static final CsvMapper CSV_MAPPER = new CsvMapper();

    // ==================== Одиночная запись ====================

    /**
     * Преобразует Map в отформатированную JSON-строку.
     */
    public static String jsonToString(Map<String, Object> jsonResult) {
        try {
            return OM.writeValueAsString(jsonResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации в JSON", e);
        }
    }

    /**
     * Преобразует Map в отформатированную XML-строку с корневым элементом &lt;result&gt;.
     * <p>
     * Ключи предварительно санируются (см. {@link #sanitizeXmlName}), т.к. имя XML-тега
     * не может содержать пробелы и ряд других символов.
     */
    public static String jsonToXmlString(Map<String, Object> jsonResult) {
        try {
            Map<String, Object> safeMap = sanitizeKeys(jsonResult);
            return XML_MAPPER.writer().withRootName("result").writeValueAsString(safeMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации в XML", e);
        }
    }

    /**
     * Преобразует Map в CSV-строку: строка заголовков + одна строка данных.
     * Вложенные структуры уплощаются (см. {@link #flatten}).
     */
    public static String jsonToCsvString(Map<String, Object> jsonResult) {
        try {
            Map<String, Object> flatMap = flatten(jsonResult, "");

            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (String key : flatMap.keySet()) {
                schemaBuilder.addColumn(key);
            }
            CsvSchema schema = schemaBuilder.build().withHeader();

            return CSV_MAPPER.writer(schema).writeValueAsString(flatMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации в CSV", e);
        }
    }

    // ==================== Список записей ====================

    /**
     * Преобразует список Map в отформатированную JSON-строку (массив объектов).
     */
    public static String jsonListToString(List<Map<String, Object>> jsonResults) {
        try {
            return OM.writeValueAsString(jsonResults);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации в JSON", e);
        }
    }

    /**
     * Преобразует список Map в отформатированную XML-строку вида
     * &lt;results&gt;&lt;result&gt;...&lt;/result&gt;...&lt;/results&gt;.
     * <p>
     * Ключи предварительно санируются (см. {@link #sanitizeXmlName}).
     */
    public static String jsonListToXmlString(List<Map<String, Object>> jsonResults) {
        try {
            // Санируем ключи каждой записи перед сериализацией
            List<Map<String, Object>> safeResults = new ArrayList<>();
            if (jsonResults != null) {
                for (Map<String, Object> map : jsonResults) {
                    safeResults.add(sanitizeKeys(map));
                }
            }

            // Оборачиваем список в Map с ключом "result", чтобы получить
            // <results><result>...</result><result>...</result></results>
            // вместо безымянных повторяющихся элементов на верхнем уровне.
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("result", safeResults);

            return XML_MAPPER.writer().withRootName("results").writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации в XML", e);
        }
    }

    /**
     * Преобразует список Map в CSV-строку: строка заголовков + по одной строке данных
     * на каждый элемент списка. Набор колонок — объединение ключей всех записей.
     */
    public static String jsonListToCsvString(List<Map<String, Object>> jsonResults) {
        try {
            if (jsonResults == null || jsonResults.isEmpty()) {
                return "";
            }

            // Уплощаем каждую запись отдельно
            List<Map<String, Object>> flatList = new ArrayList<>();
            for (Map<String, Object> map : jsonResults) {
                flatList.add(flatten(map, ""));
            }

            // Собираем ОБЪЕДИНЕНИЕ ключей по всем записям (а не только по первой),
            // т.к. у разных записей набор полей может отличаться.
            // LinkedHashSet сохраняет порядок появления колонок.
            Set<String> allColumns = new LinkedHashSet<>();
            for (Map<String, Object> flatMap : flatList) {
                allColumns.addAll(flatMap.keySet());
            }

            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (String column : allColumns) {
                schemaBuilder.addColumn(column);
            }
            CsvSchema schema = schemaBuilder.build().withHeader();

            return CSV_MAPPER.writer(schema).writeValueAsString(flatList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации в CSV", e);
        }
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Рекурсивно "уплощает" вложенные Map и List в плоскую структуру
     * с ключами вида "parent.child" и "list[0].field", т.к. CSV не умеет
     * во вложенность — только плоские строки/колонки.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> flatten(Map<String, Object> map, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                result.putAll(flatten((Map<String, Object>) value, key));
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    String indexedKey = key + "[" + i + "]";
                    if (item instanceof Map) {
                        result.putAll(flatten((Map<String, Object>) item, indexedKey));
                    } else {
                        result.put(indexedKey, item);
                    }
                }
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Рекурсивно обходит структуру и заменяет ключи Map на безопасные XML-имена.
     * Структура (вложенность Map и List) сохраняется без изменений — меняются только ключи.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitizeKeys(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (map == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String safeKey = sanitizeXmlName(entry.getKey());
            result.put(safeKey, sanitizeValue(entry.getValue()));
        }

        return result;
    }

    /**
     * Обрабатывает значение произвольного типа: для Map и List уходит вглубь,
     * остальные значения возвращает как есть.
     */
    @SuppressWarnings("unchecked")
    private static Object sanitizeValue(Object value) {
        if (value instanceof Map) {
            return sanitizeKeys((Map<String, Object>) value);
        } else if (value instanceof List) {
            List<Object> resultList = new ArrayList<>();
            for (Object item : (List<?>) value) {
                resultList.add(sanitizeValue(item));
            }
            return resultList;
        } else {
            return value;
        }
    }

    /**
     * Приводит произвольную строку к корректному имени XML-элемента:
     * <ul>
     *   <li>пробелы и любые недопустимые символы заменяются на '_';</li>
     *   <li>если имя начинается с цифры, '.' или '-', добавляется префикс '_'
     *       (XML-имя не может начинаться с этих символов);</li>
     *   <li>пустое имя заменяется на "_".</li>
     * </ul>
     */
    private static String sanitizeXmlName(String name) {
        if (name == null || name.isEmpty()) {
            return "_";
        }

        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            // допустимые символы внутри XML-имени: буква, цифра, '_', '-', '.'
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }

        // XML-имя не может начинаться с цифры, '-', '.' или быть пустым
        char first = sb.charAt(0);
        if (Character.isDigit(first) || first == '-' || first == '.') {
            sb.insert(0, '_');
        }

        return sb.toString();
    }
}