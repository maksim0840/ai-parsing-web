package io.github.maksim0840.extractionresults.integration;

import io.github.maksim0840.extractionresults.mapper.FormatsMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для FormatsMapper.
 * Проверяют сериализацию Map / List&lt;Map&gt; в JSON, XML и CSV,
 * а также санацию XML-имён (ключи с пробелами и прочими недопустимыми символами).
 */
class FormatsMapperTest {

    // ==================== Вспомогательные фабрики ====================

    private static Map<String, Object> ordered(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    // ==================== JSON ====================

    /*
    Проверяет базовую сериализацию Map в JSON:
    - все ключи и значения присутствуют в результате
    - включено человекочитаемое форматирование (есть переносы строк)
    */
    @Test
    void jsonToStringSerializesAllFields() {
        Map<String, Object> source = ordered("name", "Boris", "age", 30);

        String json = FormatsMapper.jsonToString(source);

        assertThat(json).contains("\"name\"").contains("Boris").contains("\"age\"").contains("30");
        assertThat(json).contains("\n");
    }

    /*
    Проверяет сериализацию вложенной структуры в JSON:
    - вложенные Map и List не "сплющиваются"
    - значения всех уровней попадают в результат
    */
    @Test
    void jsonToStringKeepsNestedStructure() {
        Map<String, Object> source = ordered(
                "car", ordered("model", "Ford", "year", 2014),
                "tags", List.of("a", "b")
        );

        String json = FormatsMapper.jsonToString(source);

        assertThat(json).contains("\"car\"").contains("Ford").contains("2014");
        assertThat(json).contains("\"tags\"").contains("\"a\"").contains("\"b\"");
    }

    /*
    Проверяет сериализацию списка записей в JSON:
    - результат является JSON-массивом
    - содержимое каждой записи присутствует
    */
    @Test
    void jsonListToStringProducesArray() {
        List<Map<String, Object>> source = List.of(
                ordered("id", 1),
                ordered("id", 2)
        );

        String json = FormatsMapper.jsonListToString(source);

        assertThat(json.trim()).startsWith("[").endsWith("]");
        assertThat(json).contains("1").contains("2");
    }

    // ==================== XML: базовая структура ====================

    /*
    Проверяет базовую сериализацию Map в XML:
    - присутствует XML-декларация
    - корневой элемент называется "result"
    - поля становятся дочерними тегами
    */
    @Test
    void jsonToXmlStringWrapsInResultRoot() {
        Map<String, Object> source = ordered("name", "Boris");

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("<?xml");
        assertThat(xml).contains("<result").contains("</result>");
        assertThat(xml).contains("<name>Boris</name>");
    }

    /*
    Проверяет сериализацию списка записей в XML:
    - корневой элемент "results"
    - каждая запись обёрнута в отдельный тег "result"
    */
    @Test
    void jsonListToXmlStringWrapsEachRecord() {
        List<Map<String, Object>> source = List.of(
                ordered("name", "Boris"),
                ordered("name", "Anna")
        );

        String xml = FormatsMapper.jsonListToXmlString(source);

        assertThat(xml).contains("<results").contains("</results>");
        assertThat(xml).contains("Boris").contains("Anna");
        // два вхождения открывающего тега <result>
        assertThat(xml.split("<result>", -1).length - 1).isEqualTo(2);
    }

    /*
    Проверяет поведение jsonListToXmlString на пустом списке:
    - не падает
    - возвращает корректный XML с корневым элементом
    */
    @Test
    void jsonListToXmlStringHandlesEmptyList() {
        String xml = FormatsMapper.jsonListToXmlString(List.of());

        assertThat(xml).contains("results");
    }

    // ==================== XML: санация имён тегов ====================

    /*
    Ключевой тест по багу с пробелами в именах тегов.
    Проверяет, что ключ "Нижний Новгород" превращается в валидный тег
    "Нижний_Новгород", а значение при этом не теряется.
    */
    @Test
    void jsonToXmlStringReplacesSpacesInKeys() {
        Map<String, Object> source = ordered("Нижний Новгород", "22 сентября 2025");

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("<Нижний_Новгород>22 сентября 2025</Нижний_Новгород>");
        assertThat(xml).doesNotContain("<Нижний Новгород>");
    }

    /*
    Проверяет, что кириллица в ключах сохраняется:
    - буквы не заменяются на подчёркивания
    - заменяется только недопустимый символ
    */
    @Test
    void jsonToXmlStringKeepsCyrillicLetters() {
        Map<String, Object> source = ordered("Дата рождения", "01.01.2000");

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("<Дата_рождения>");
    }

    /*
    Проверяет замену прочих недопустимых для XML-имени символов
    (скобки, слэш, знак вопроса, двоеточие) на подчёркивание.
    */
    @Test
    void jsonToXmlStringReplacesInvalidCharacters() {
        Map<String, Object> source = ordered("price (USD)", 10, "a/b?c", "x");

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("<price__USD_>");
        assertThat(xml).contains("<a_b_c>");
    }

    /*
    Проверяет, что имя, начинающееся с цифры, получает префикс "_":
    XML-имя не может начинаться с цифры.
    */
    @Test
    void jsonToXmlStringPrefixesKeysStartingWithDigit() {
        Map<String, Object> source = ordered("2025 год", "данные");

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("<_2025_год>");
    }

    /*
    Проверяет, что пустой ключ заменяется на "_" и не ломает документ.
    */
    @Test
    void jsonToXmlStringHandlesEmptyKey() {
        Map<String, Object> source = ordered("", "value");

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("<_>value</_>");
    }

    /*
    Проверяет, что санация работает рекурсивно для вложенных Map:
    ключи на всех уровнях вложенности должны быть исправлены.
    */
    @Test
    void jsonToXmlStringSanitizesNestedKeys() {
        Map<String, Object> source = ordered(
                "outer key", ordered("inner key", "value")
        );

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("<outer_key>");
        assertThat(xml).contains("<inner_key>value</inner_key>");
    }

    /*
    Проверяет, что санация работает для Map внутри List:
    ключи объектов внутри массива тоже должны быть исправлены.
    */
    @Test
    void jsonToXmlStringSanitizesKeysInsideLists() {
        Map<String, Object> source = ordered(
                "items", List.of(
                        ordered("item name", "первый"),
                        ordered("item name", "второй")
                )
        );

        String xml = FormatsMapper.jsonToXmlString(source);

        assertThat(xml).contains("item_name");
        assertThat(xml).contains("первый").contains("второй");
        assertThat(xml).doesNotContain("item name>");
    }

    /*
    Проверяет, что санация применяется и к списочному варианту (jsonListToXmlString),
    а не только к одиночной записи.
    */
    @Test
    void jsonListToXmlStringSanitizesKeys() {
        List<Map<String, Object>> source = List.of(
                ordered("Нижний Новгород", "22 сентября 2025")
        );

        String xml = FormatsMapper.jsonListToXmlString(source);

        assertThat(xml).contains("Нижний_Новгород");
        assertThat(xml).doesNotContain("<Нижний Новгород>");
    }

    /*
    Проверяет, что итоговый XML разбирается стандартным парсером,
    то есть является well-formed документом.
    Это регрессия на исходную ошибку "XML Parsing Error: not well-formed".
    */
    @Test
    void jsonToXmlStringProducesWellFormedDocument() throws Exception {
        Map<String, Object> source = ordered(
                "Нижний Новгород", "22 сентября 2025",
                "2025 год", ordered("под ключ", "да")
        );

        String xml = FormatsMapper.jsonToXmlString(source);

        javax.xml.parsers.DocumentBuilderFactory factory =
                javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        org.w3c.dom.Document document = factory.newDocumentBuilder().parse(
                new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );

        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("result");
    }

    // ==================== CSV ====================

    /*
    Проверяет базовую сериализацию Map в CSV:
    - первая строка содержит заголовки колонок в порядке ключей
    - вторая строка содержит значения
    */
    @Test
    void jsonToCsvStringProducesHeaderAndRow() {
        Map<String, Object> source = ordered("name", "Boris", "age", 30);

        String csv = FormatsMapper.jsonToCsvString(source);
        String[] lines = csv.trim().split("\\R");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo("name,age");
        assertThat(lines[1]).isEqualTo("Boris,30");
    }

    /*
    Проверяет уплощение вложенных Map при сериализации в CSV:
    вложенные поля становятся колонками вида "parent.child".
    */
    @Test
    void jsonToCsvStringFlattensNestedMaps() {
        Map<String, Object> source = ordered(
                "id", 1,
                "car", ordered("model", "Ford", "year", 2014)
        );

        String csv = FormatsMapper.jsonToCsvString(source);
        String[] lines = csv.trim().split("\\R");

        assertThat(lines[0]).isEqualTo("id,car.model,car.year");
        assertThat(lines[1]).isEqualTo("1,Ford,2014");
    }

    /*
    Проверяет уплощение списков при сериализации в CSV:
    элементы списка становятся колонками вида "field[0]", "field[1]".
    */
    @Test
    void jsonToCsvStringFlattensLists() {
        Map<String, Object> source = ordered("tags", List.of("a", "b"));

        String csv = FormatsMapper.jsonToCsvString(source);
        String[] lines = csv.trim().split("\\R");

        assertThat(lines[0]).isEqualTo("tags[0],tags[1]");
        assertThat(lines[1]).isEqualTo("a,b");
    }

    /*
    Проверяет уплощение списка объектов:
    поля объектов внутри списка становятся колонками "field[0].sub".
    */
    @Test
    void jsonToCsvStringFlattensListsOfMaps() {
        Map<String, Object> source = ordered(
                "items", List.of(
                        ordered("sku", "A1"),
                        ordered("sku", "B2")
                )
        );

        String csv = FormatsMapper.jsonToCsvString(source);
        String[] lines = csv.trim().split("\\R");

        assertThat(lines[0]).isEqualTo("items[0].sku,items[1].sku");
        assertThat(lines[1]).isEqualTo("A1,B2");
    }

    /*
    Проверяет, что jsonListToCsvString собирает объединение колонок
    по всем записям, а не только по первой.
    Запись без какого-то поля получает пустое значение в этой колонке.
    */
    @Test
    void jsonListToCsvStringUnionsColumnsAcrossRecords() {
        List<Map<String, Object>> source = List.of(
                ordered("a", 1, "b", 2),
                ordered("b", 3, "c", 4)
        );

        String csv = FormatsMapper.jsonListToCsvString(source);
        String[] lines = csv.trim().split("\\R");

        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("a,b,c");
        assertThat(lines[1]).startsWith("1,2");
        assertThat(lines[2]).contains("3").contains("4");
    }

    /*
    Проверяет, что jsonListToCsvString на пустом списке
    возвращает пустую строку, а не падает.
    */
    @Test
    void jsonListToCsvStringReturnsEmptyStringForEmptyList() {
        assertThat(FormatsMapper.jsonListToCsvString(List.of())).isEmpty();
    }

    /*
    Проверяет, что jsonListToCsvString корректно обрабатывает null
    (защитная ветка в начале метода).
    */
    @Test
    void jsonListToCsvStringReturnsEmptyStringForNull() {
        assertThat(FormatsMapper.jsonListToCsvString(null)).isEmpty();
    }
}
