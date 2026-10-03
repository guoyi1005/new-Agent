package com.example.appbackend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobTitleNormalizerTest {
    private final JobTitleNormalizer normalizer = new JobTitleNormalizer();

    @Test void normalizesEquivalentJavaTitles() {
        assertEquals("Java后端开发", normalizer.normalize("Java后端工程师"));
        assertEquals("Java后端开发", normalizer.normalize("Java开发工程师"));
        assertEquals("Java后端开发", normalizer.normalize("后端开发工程师（Java）"));
    }

    @Test void normalizesPythonTitles() {
        assertEquals("Python开发工程师", normalizer.normalize("Python工程师"));
        assertEquals("Python开发工程师", normalizer.normalize("Python后端工程师"));
    }
}
