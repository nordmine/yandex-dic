package ru.nordmine.yandex.dic;

import org.apache.log4j.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Program {

    public static final String EN_WORD_PATTERN = "^([a-z]|[a-z][a-z\\-]{0,23}[a-z])$"; // todo синхронизировать регулярку во всех проектах

    private static final Logger logger = Logger.getLogger(Program.class);

    public static void main(String[] args) throws Exception {
        if (args.length >= 4) {
            String apiKey = args[0];

            String wordListFileName = args[1];
            File wordListFile = new File(wordListFileName);
            if (!wordListFile.exists()) {
                logger.error("specified word list file doesn't exists");
                return;
            }

            String outputDir = args[2];
            File wordsDirFile = new File(outputDir);
            if (!wordsDirFile.exists()) {
                logger.error("specified directory doesn't exists");
                return;
            }

            String operation = args[3];

            List<String> lines = Files.readAllLines(wordListFile.toPath(), Charset.forName("utf-8"));
            List<String> words = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(",");
                String word = parts[0].trim().toLowerCase();
                if (word.matches(EN_WORD_PATTERN)) {
                    words.add(word);
                }
            }
            logger.info(words.size() + " words loaded from file");

            switch (operation) {
                case "download": new ApiDownloader().parseWords(words, outputDir, apiKey); break;
                case "analyze": new ResponseAnalyzer().processFiles(words, outputDir); break;
            }
        } else {
            logger.info("1st param - Yandex API key");
            logger.info("2nd param - word list file name");
            logger.info("3rd param - absolute path for words directory (with / at the and)");
            logger.info("4th param - operation (download|analyze)");
        }
    }
}
