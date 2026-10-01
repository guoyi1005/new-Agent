package com.example.appbackend.service.exampaper;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourcePaperTemplateResourcesTest {

    private static final String ROOT = "exam-paper-template/";
    private static final Map<String, String> SOURCE_SHA_256 = Map.of(
            "static/document.docx", "3a403d4baa8160d9f2d688762013dc56bbc72d25f07efcf7403a2270e926302e",
            "document/document.xml", "326a6d8ef46b05542583606876d9484223bfac6ed59a8dcf7e3c885f0c3835a9",
            "document/document.xml.rels", "853a2c17681a2f1c889a95552d1615ef22363be13cd8a132b9f0b5de9667f4e7",
            "head/header1.xml", "12b626501fba5938207bce415b4deaa06a9c16291b258fee291bfd5985b71710",
            "head/header2.xml", "58ddfca96276bfd12d17f5e1ae55feebc14999537929b811624386545010e1e6"
    );
    private static final List<String> IMPORTED_XML_PARTS = List.of(
            "document/document.xml",
            "document/document.xml.rels",
            "head/header1.xml",
            "head/header2.xml"
    );
    private static final Set<String> REQUIRED_PACKAGE_ENTRIES = Set.of(
            "word/document.xml",
            "word/header1.xml",
            "word/header2.xml",
            "word/footer1.xml",
            "word/footer2.xml",
            "word/styles.xml",
            "word/numbering.xml",
            "word/settings.xml",
            "word/_rels/document.xml.rels"
    );
    private static final List<String> REQUIRED_PLACEHOLDERS = List.of(
            "%TITLE%", "%SUBTITLE%", "%TIME%", "%SCORE%", "%NAME%",
            "%PRECAUTIONS%", "%HEADER%", "%QUESTION%", "%ANSWER%", "%PageSetting%"
    );

    @Test
    void importsExactSourceAssets() throws Exception {
        for (Map.Entry<String, String> asset : SOURCE_SHA_256.entrySet()) {
            byte[] bytes = resource(asset.getKey());
            assertEquals(asset.getValue(), sha256(bytes), asset.getKey() + " must remain byte-for-byte source-faithful");
        }

        for (String xmlPart : IMPORTED_XML_PARTS) {
            assertNotNull(parse(resource(xmlPart)), xmlPart + " must be well-formed XML");
        }
    }

    @Test
    void sourceDocxContainsRequiredWordPackageParts() throws Exception {
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(resource("static/document.docx")))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }

        assertTrue(entries.containsAll(REQUIRED_PACKAGE_ENTRIES),
                () -> "Missing required DOCX entries: " + difference(REQUIRED_PACKAGE_ENTRIES, entries));
    }

    @Test
    void documentTemplateContainsEveryRequiredPlaceholderExactlyOnce() throws Exception {
        Document document = parse(resource("document/document.xml"));
        String text = document.getDocumentElement().getTextContent();

        for (String placeholder : REQUIRED_PLACEHOLDERS) {
            assertEquals(1, occurrences(text, placeholder), placeholder + " must occur exactly once");
        }
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream input = SourcePaperTemplateResourcesTest.class.getClassLoader().getResourceAsStream(ROOT + path)) {
            assertNotNull(input, "Missing classpath resource " + ROOT + path);
            return input.readAllBytes();
        }
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(token, index)) >= 0; index += token.length()) {
            count++;
        }
        return count;
    }

    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }
}
