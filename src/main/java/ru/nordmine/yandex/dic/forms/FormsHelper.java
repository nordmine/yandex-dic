package ru.nordmine.yandex.dic.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FormsHelper {

    private final Map<String, Map<FormType, Set<String>>> specialSuffixes;

    public FormsHelper(Map<String, Map<FormType, Set<String>>> specialSuffixes) {
        this.specialSuffixes = specialSuffixes;
    }

    public FormType getFormType(String lemma, String wordForm) {
//        if (checkSpecialWordsFor(FormType.IR2, lemma, wordForm)) {
//            return FormType.IR2;
//        }
//        if (checkSpecialWordsFor(FormType.IR3, lemma, wordForm)) {
//            return FormType.IR3;
//        }
        if (isEdForm(lemma, wordForm)) {
            return FormType.ED;
        }
        if (isPlural(lemma, wordForm)) {
            return FormType.PL;
        }
        if (isErForm(lemma, wordForm)) {
            return FormType.ER;
        }
        if (isEstForm(lemma, wordForm)) {
            return FormType.EST;
        }
        if (isIngForm(lemma, wordForm)) {
            return FormType.ING;
        }
        return null;
    }

    private boolean isPlural(String lemma, String wordForm) {
        FormType formType = FormType.PL;
        if (checkSpecialSuffixes(formType, lemma, wordForm)) {
            return true;
        }

        List<String> possibleForms = new ArrayList<>();
        possibleForms.add(lemma + "s");
        possibleForms.add(lemma + "es");
        possibleForms.add(lemma + "e");
        if (lemma.length() > 1) {
            possibleForms.add(lemma.substring(0, lemma.length() - 1) + "ies");
            possibleForms.add(lemma.substring(0, lemma.length() - 1) + "ves");
        }
        if (lemma.length() > 2) {
            possibleForms.add(lemma.substring(0, lemma.length() - 2) + "ves");
        }
        return possibleForms.contains(wordForm);
    }

    private boolean checkSpecialSuffixes(FormType formType, String lemma, String wordForm) {
        for (Map.Entry<String, Map<FormType, Set<String>>> entry : specialSuffixes.entrySet()) {
            if (lemma.endsWith(entry.getKey())) {
                if (entry.getValue().containsKey(formType)) {
                    for (String form : entry.getValue().get(formType)) {
                        if (wordForm.endsWith(form)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isEdForm(String lemma, String wordForm) {
        List<String> possibleForms = new ArrayList<>();
        possibleForms.add(lemma + "d");
        possibleForms.add(lemma + "ed");
        possibleForms.add(lemma + "led");
        possibleForms.add(lemma + "ked");
        if (lemma.length() > 1) {
            possibleForms.add(lemma.substring(0, lemma.length() - 1) + "ied");
            possibleForms.add(lemma + lemma.charAt(lemma.length() - 1) + "ed");
        }
        return possibleForms.contains(wordForm);
    }

    private boolean isErForm(String lemma, String wordForm) {
        if (checkSpecialSuffixes(FormType.ER, lemma, wordForm)) {
            return true;
        }

        List<String> possibleForms = new ArrayList<>();
        possibleForms.add(lemma + "er");
        possibleForms.add(lemma + "r");
        if (lemma.length() > 1) {
            possibleForms.add(lemma + lemma.charAt(lemma.length() - 1) + "er");
            possibleForms.add(lemma.substring(0, lemma.length() - 1) + "ier");
        }
        return possibleForms.contains(wordForm);
    }

    private boolean isEstForm(String lemma, String wordForm) {
        if (checkSpecialSuffixes(FormType.EST, lemma, wordForm)) {
            return true;
        }

        List<String> possibleForms = new ArrayList<>();
        possibleForms.add(lemma + "est");
        possibleForms.add(lemma + "st");
        if (lemma.length() > 1) {
            possibleForms.add(lemma + lemma.charAt(lemma.length() - 1) + "est");
            possibleForms.add(lemma.substring(0, lemma.length() - 1) + "iest");
        }
        return possibleForms.contains(wordForm);
    }

    public boolean isIngForm(String lemma, String wordForm) {
        List<String> possibleForms = new ArrayList<>();
        possibleForms.add(lemma + "ing");
        possibleForms.add(lemma + "king");
        if (lemma.length() > 1) {
            possibleForms.add(lemma + lemma.charAt(lemma.length() - 1) + "ing");
            possibleForms.add(lemma.substring(0, lemma.length() - 1) + "ing");
        }
        if (lemma.length() > 2) {
            possibleForms.add(lemma.substring(0, lemma.length() - 2) + "ying");
        }
        return possibleForms.contains(wordForm);
    }
}
