package ru.nordmine.yandex.dic.utils;

import java.util.List;
import java.util.Set;

public interface LinePartsProcessor {
    void process(String lemma, List<Set<String>> forms);
}
