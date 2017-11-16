package ru.nordmine.yandex.dic;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import ru.nordmine.yandex.dic.forms.FormType;
import ru.nordmine.yandex.dic.forms.FormsHelper;
import ru.nordmine.yandex.dic.utils.LinePartsProcessor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ResponseAnalyzer {

    private static final Logger logger = Logger.getLogger(ResponseAnalyzer.class);

    private final Map<String, Map<FormType, Set<String>>> parsedForms = new HashMap<>();
    private final Set<String> knownSpeechParts = new HashSet<>();
    private final Map<String, Set<String>> unknownForms = new HashMap<>();
    private final Set<String> unboundWords = new HashSet<>();
    private final Set<String> oldWords = new HashSet<>();

    private final Map<String, Map<FormType, Set<String>>> specialSuffixes = new HashMap<>();

    private FormsHelper formsHelper;

    public void processFiles(List<String> words, String outputDir) throws Exception {
        if (specialSuffixes.isEmpty()) {
            readExclusions("/plurals.txt", (lemma, forms) -> {
                if (!specialSuffixes.containsKey(lemma)) {
                    specialSuffixes.put(lemma, new HashMap<>());
                }
                specialSuffixes.get(lemma).put(FormType.PL, forms.get(0));
            });

            readExclusions("/irregular-verbs.txt", (lemma, forms) -> {
                if (!specialSuffixes.containsKey(lemma)) {
                    specialSuffixes.put(lemma, new HashMap<>());
                }
                specialSuffixes.get(lemma).put(FormType.IR2, forms.get(0));
                specialSuffixes.get(lemma).put(FormType.IR3, forms.get(1));
            });

            readExclusions("/comparisons.txt", (lemma, forms) -> {
                if (!specialSuffixes.containsKey(lemma)) {
                    specialSuffixes.put(lemma, new HashMap<>());
                }
                specialSuffixes.get(lemma).put(FormType.ER, forms.get(0));
                specialSuffixes.get(lemma).put(FormType.EST, forms.get(1));
            });
        }

        oldWords.addAll(FileUtils.readLines(new File("/home/boris/Documents/words-from-wiki/old-words.txt"), StandardCharsets.UTF_8));

        formsHelper = new FormsHelper(specialSuffixes);

        int emptyResponseCounter = 0;
        for (String originalWord : words) {
            boolean equalityChecked = false;

            File subDirFile = new File(outputDir + File.separator + originalWord.substring(0, 1));
            if (!subDirFile.exists()) {
                logger.error("Directory " + subDirFile.getAbsolutePath() + " not found");
                return;
            }

            File wordFile = new File(subDirFile.getAbsolutePath() + File.separator + originalWord + ".xml");
            if (wordFile.exists()) {
                String xml = FileUtils.readFileToString(wordFile, UTF_8);
                Document document = DocumentHelper.parseText(xml);
                List<Node> nodes = document.selectNodes("/DicResult/def");
                if (nodes.size() == 0) {
                    emptyResponseCounter++;
                    continue;
                }

                for (Node node : nodes) {
                    String lemma = node.selectSingleNode("text").getText().toLowerCase();
                    if (!lemma.matches(Program.EN_WORD_PATTERN)) {
                        continue;
                    }

                    if (originalWord.equalsIgnoreCase(lemma)) {
                        // если имя файла не совпадает со словом внутри этого файла, значит это слово - словоформа
                        unboundWords.add(lemma);
                    }
                    // todo определять имена собственные
                    if (!equalityChecked) {
                        if (!originalWord.equals(lemma)) {
                            FormType formType = formsHelper.getFormType(lemma, originalWord);
                            if (formType == null) {
                                addToLookup(unknownForms, lemma, originalWord);
                            } else {
                                addAsForm(lemma, formType, originalWord);
                            }
                            equalityChecked = true;
                        }
                    }
                    List<Node> translationNodes = node.selectNodes("tr");
                    for (Node translationNode : translationNodes) {
                        String speechPart = translationNode.selectSingleNode("@pos").getText();
                        if (!knownSpeechParts.contains(speechPart)) {
                            knownSpeechParts.add(speechPart);
                        }
                    }
                }
            }
        }

        logger.info("unbound words before processing: " + unboundWords.size());
        processUnboundWords();

        parsedForms.keySet().stream().filter(unboundWords::contains).forEach(unboundWords::remove);

        for (Map.Entry<String, Map<FormType, Set<String>>> entry : parsedForms.entrySet()) {
            String lemma = entry.getKey();
            Map<FormType, Set<String>> parsedForm = entry.getValue();
            for (Set<String> forms : parsedForm.values()) {
                forms.stream()
                        .filter(f -> unknownForms.containsKey(lemma))
                        .filter(s -> unknownForms.get(lemma).contains(s))
                        .forEach(s -> {
                            unknownForms.get(lemma).remove(s);
                            if (unknownForms.get(lemma).isEmpty()) {
                                unknownForms.remove(lemma);
                            }
                        });
            }
        }

        Set<String> plainParsedWords = new HashSet<>(unboundWords);
        for (Map.Entry<String, Map<FormType, Set<String>>> entry : parsedForms.entrySet()) {
            plainParsedWords.add(entry.getKey());
            Map<FormType, Set<String>> parsedForm = entry.getValue();
            for (Set<String> forms : parsedForm.values()) {
                plainParsedWords.addAll(forms);
            }
        }

        plainParsedWords.stream().filter(oldWords::contains).forEach(oldWords::remove);
        unboundWords.stream().filter(oldWords::contains).forEach(oldWords::remove);

        FileUtils.writeLines(new File(outputDir + "-old-words.txt"), oldWords);

        saveWordSet(outputDir + "-unbound-words.txt", unboundWords);
        saveWordSet(outputDir + "-speech-parts.txt", knownSpeechParts);
        saveLookup(outputDir + "-unknown-forms.txt", unknownForms);
        saveParsedForms(outputDir + "-parsed-forms.txt", parsedForms);
        logger.info("unbound words: " + unboundWords.size());
        logger.info("total parsed words: " + plainParsedWords.size());
        logger.info("words with empty response: " + emptyResponseCounter);
    }

    private void processUnboundWords() {
//        Set<String> unboundPlurals = unboundWords.stream().filter(s -> s.endsWith("s")).collect(Collectors.toSet());
//        logger.info("unbound plurals: " + unboundPlurals.size());
//        Set<String> unboundEds = unboundWords.stream().filter(s -> s.endsWith("ed")).collect(Collectors.toSet());
//        logger.info("unbound eds: " + unboundEds.size());

        processIrregularVerbs();
/*
        Set<String> unboundIngs = unboundWords.stream().filter(s -> s.endsWith("ing")).collect(Collectors.toSet());
        logger.info("unbound ings: " + unboundIngs.size());

        for (String lemma : parsedForms.keySet()) {
            processUnboundSet(lemma, FormType.ING, unboundIngs); // todo определять ing по правилам
//            processUnboundSet(lemma, FormType.PL, unboundPlurals);
//            processUnboundSet(lemma, FormType.ED, unboundEds);
        }
*/
    }

    private void processIrregularVerbs() {
        processMandatoryFormTypes(FormType.IR2);
        processMandatoryFormTypes(FormType.IR3);
    }

    private void processMandatoryFormTypes(FormType formType) {
        List<String> prefixes = Arrays.asList("a", "be", "co-", "fore", "mis", "re", "out", "over", "un", "under", "with", "cross");
        for (Map.Entry<String, Map<FormType, Set<String>>> entry : specialSuffixes.entrySet()) {
            String lemma = entry.getKey();
            if (entry.getValue().containsKey(formType)) {
                entry.getValue().get(formType).forEach(s -> {
                    addAsForm(lemma, formType, s);

                    for (String prefix : prefixes) {
                        String prefixLemma = prefix + lemma;
                        String prefixForm = prefix + s;
                        if (unknownForms.containsKey(prefixLemma)) {
                            if (unknownForms.get(prefixLemma).contains(prefixForm)) {
                                addAsForm(prefixLemma, formType, prefixForm);
                            }
                        }
                        if (unboundWords.contains(prefixForm)) {
                            addAsForm(prefixLemma, formType, prefixForm);
                        }
                    }
                });
            }
        }
    }

    private void processUnboundSet(String lemma, FormType formType, Set<String> unboundSet) {
        if (!parsedForms.get(lemma).containsKey(formType)) {
            for (String unboundWord : unboundSet) {
                if (formType == FormType.ING && formsHelper.isIngForm(lemma, unboundWord)) {
                    addAsForm(lemma, formType, unboundWord);
                    unboundWords.remove(unboundWord);
                    return;
                }
            }
        }
    }

    private void addAsForm(String lemma, FormType formType, String form) {
        if (!parsedForms.containsKey(lemma)) {
            parsedForms.put(lemma, new HashMap<>());
        }
        Map<FormType, Set<String>> formsMap = parsedForms.get(lemma);
        if (!formsMap.containsKey(formType)) {
            formsMap.put(formType, new HashSet<>());
        }
        formsMap.get(formType).add(form);
    }

    private static void addToLookup(Map<String, Set<String>> lookup, String key, String value) {
        if (!lookup.containsKey(key)) {
            lookup.put(key, new HashSet<>());
        }
        lookup.get(key).add(value);
    }

    private void readExclusions(String resourceFileName, LinePartsProcessor processor) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceFileName);
        new BufferedReader(new InputStreamReader(is))
                .lines()
                .forEachOrdered(line -> {
                    String[] parts = line.split("\\|");
                    String lemma = parts[0];
                    List<Set<String>> forms = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        String part = parts[i];
                        Set<String> formSet = new HashSet<>();
                        String[] formArray = part.split(",");
                        formSet.addAll(Arrays.asList(formArray));
                        forms.add(formSet);
                    }
                    processor.process(lemma, forms);
                });
    }

    private static void saveWordSet(String fileName, Set<String> wordSet) throws IOException {
        Path path = Paths.get(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
            for (String speechPart : wordSet) {
                writer.write(String.format("%s%s", speechPart, System.lineSeparator()));
            }
        }
    }

    private static void saveLookup(String fileName, Map<String, Set<String>> lookup) throws IOException {
        Path resultPath = Paths.get(fileName);
        int counter = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(resultPath, UTF_8)) {
            for (Map.Entry<String, Set<String>> entry : lookup.entrySet()) {
                writer.write(String.format("%s:%s%s", entry.getKey(), entry.getValue().stream().collect(Collectors.joining(",")), System.lineSeparator()));
                counter++;
            }
        }
        logger.info(counter + " rows saved in " + resultPath.getFileName());
    }

    private static void saveParsedForms(String fileName, Map<String, Map<FormType, Set<String>>> parsedForms) throws IOException {
        Path resultPath = Paths.get(fileName);
        int counter = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(resultPath, UTF_8)) {
            for (Map.Entry<String, Map<FormType, Set<String>>> entry : parsedForms.entrySet()) {
                List<String> elements = new ArrayList<>();
                addElementIfExists(entry.getValue(), FormType.IR2, elements);
                addElementIfExists(entry.getValue(), FormType.IR3, elements);
                addElementIfExists(entry.getValue(), FormType.ED, elements);
                addElementIfExists(entry.getValue(), FormType.PL, elements);
                addElementIfExists(entry.getValue(), FormType.ER, elements);
                addElementIfExists(entry.getValue(), FormType.EST, elements);
                addElementIfExists(entry.getValue(), FormType.ING, elements);
                writer.write(String.format("%s|%s%s", entry.getKey(), elements.stream().collect(Collectors.joining("|")), System.lineSeparator()));
                counter++;
            }
        }
        logger.info(counter + " rows saved in " + resultPath.getFileName());
    }

    private static void addElementIfExists(Map<FormType, Set<String>> elements, FormType key, List<String> dest) {
        if (elements.containsKey(key) && !elements.get(key).isEmpty()) {
            dest.add(elements.get(key).stream().collect(Collectors.joining(",")) + "(" + key + ")");
        }
    }
}
