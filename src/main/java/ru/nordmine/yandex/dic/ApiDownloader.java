package ru.nordmine.yandex.dic;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApiDownloader {

    private static final Logger logger = Logger.getLogger(ApiDownloader.class);

    private static final String COMMON_URL = "https://dictionary.yandex.net/api/v1/dicservice/lookup?key=API_KEY&lang=en-ru&flags=5&ui=en&text=";

    private static final int ERR_OK = 200; // Операция выполнена успешно
    private static final int ERR_KEY_INVALID = 401; // Ключ API невалиден
    private static final int ERR_KEY_BLOCKED = 402; // Ключ API заблокирован
    private static final int ERR_DAILY_REQ_LIMIT_EXCEEDED = 403; // Превышено суточное ограничение на количество запросов
    private static final int ERR_TEXT_TOO_LONG = 413; // Превышен максимальный размер текста

    public void parseWords(List<String> words, String outputDir, String apiKey) throws Exception {
        int counter = 1;
        for (String word : words) {
            File subDirFile = new File(outputDir + File.separator + word.substring(0, 1));
            if (!subDirFile.exists()) {
                subDirFile.mkdir();
            }
            File wordFile = new File(subDirFile.getAbsolutePath() + File.separator + word + ".xml");
            if (!wordFile.exists()) {
                HttpResponse response = RequestHelper.executeGetRequest(COMMON_URL.replace("API_KEY", apiKey) + URLEncoder.encode(word, "utf-8"));
                int statusCode = response.getStatusLine().getStatusCode();
                switch (statusCode) {
                    case ERR_OK:
                        String responseBody = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
                        logger.info(String.format("Process: %s (%s in %s)", word, counter, words.size()));
                        FileUtils.writeStringToFile(wordFile, responseBody, StandardCharsets.UTF_8);
                        break;
                    case ERR_KEY_INVALID:
                        logger.error("Api key invalid!");
                        return;
                    case ERR_KEY_BLOCKED:
                        logger.error("Api key blocked!");
                        return;
                    case ERR_DAILY_REQ_LIMIT_EXCEEDED:
                        logger.warn("Daily limit exceeded");
                        return;
                    case ERR_TEXT_TOO_LONG:
                        logger.warn("Text too long for word " + word);
                        break;
                    case 404:
                        logger.error("Word " + word + " not found");
                        break;
                    case 302:
                        logger.warn("Redirect for " + word + ": " + response.getFirstHeader("Location").getValue());
                        break;
                    default:
                        logger.error("Unknown status code: " + statusCode);
                        return;
                }
                TimeUnit.MILLISECONDS.sleep(250);
            }
            counter++;
        }
    }
}
