package com.xinchang.management.template.service;

import com.xinchang.management.common.BusinessException;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocRenderServiceTest {

    private DocRenderService renderService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        renderService = new DocRenderService();
    }

    // ==================== Basic replacement ====================

    @Test
    void render_SimplePlaceholder_Replaced() throws IOException {
        Path template = createTemplate("#{companyName}");

        Map<String, String> fields = new HashMap<>();
        fields.put("companyName", "新长科技有限公司");

        byte[] result = renderService.render(template.toString(), fields);
        String content = extractText(result);

        assertTrue(content.contains("新长科技有限公司"));
        assertFalse(content.contains("#{companyName}"));
    }

    @Test
    void render_MultipleFields_AllReplaced() throws IOException {
        Path template = createTemplate("#{companyName} - #{legalPerson}");

        Map<String, String> fields = new HashMap<>();
        fields.put("companyName", "测试企业");
        fields.put("legalPerson", "张三");

        byte[] result = renderService.render(template.toString(), fields);
        String content = extractText(result);

        assertTrue(content.contains("测试企业 - 张三"));
        assertFalse(content.contains("#{companyName}"));
        assertFalse(content.contains("#{legalPerson}"));
    }

    // ==================== Missing field ====================

    @Test
    void render_MissingField_LeftEmpty() throws IOException {
        Path template = createTemplate("#{companyName}（#{missingField}）");

        Map<String, String> fields = new HashMap<>();
        fields.put("companyName", "测试企业");

        byte[] result = renderService.render(template.toString(), fields);
        String content = extractText(result);

        assertTrue(content.contains("测试企业（）"));
        assertFalse(content.contains("#{companyName}"));
    }

    // ==================== Footer replacement ====================

    @Test
    void render_FooterPlaceholder_Replaced() throws IOException {
        Path template = createTemplateWithFooter("#{companyName}");

        Map<String, String> fields = new HashMap<>();
        fields.put("companyName", "页脚企业名");

        byte[] result = renderService.render(template.toString(), fields);
        String content = extractText(result);

        assertTrue(content.contains("页脚企业名"));
        assertFalse(content.contains("#{companyName}"));
    }

    // ==================== Original file unchanged ====================

    @Test
    void render_OriginalFileUnchanged() throws IOException {
        Path template = createTemplate("#{companyName}");

        byte[] originalBytes = Files.readAllBytes(template);

        Map<String, String> fields = new HashMap<>();
        fields.put("companyName", "新长科技有限公司");
        renderService.render(template.toString(), fields);

        byte[] afterBytes = Files.readAllBytes(template);
        assertArrayEquals(originalBytes, afterBytes,
                "原始模板文件不应被修改");
    }

    // ==================== Corrupted template ====================

    @Test
    void render_CorruptedTemplate_ThrowsBusinessException() throws IOException {
        Path badFile = tempDir.resolve("bad.docx");
        Files.write(badFile, "not a valid docx".getBytes());

        Map<String, String> fields = new HashMap<>();
        fields.put("companyName", "test");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> renderService.render(badFile.toString(), fields));
        assertTrue(ex.getMessage().contains("模板异常"));
    }

    // ==================== Field map builders ====================

    @Test
    void buildFieldMap_NullValues_EmptyString() {
        com.xinchang.management.company.entity.Company company =
                new com.xinchang.management.company.entity.Company();
        company.setCompanyName("测试企业");

        Map<String, String> map = renderService.buildCompanyFieldMap(company);
        assertEquals("测试企业", map.get("companyName"));
        assertEquals("", map.get("legalPerson"));
        assertEquals("", map.get("contactPhone"));
    }

    // ==================== Helpers ====================

    private Path createTemplate(String content) throws IOException {
        Path file = tempDir.resolve("template.docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(content);
            try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                doc.write(fos);
            }
        }
        return file;
    }

    private Path createTemplateWithFooter(String content) throws IOException {
        Path file = tempDir.resolve("footer-template.docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setText("正文内容");

            XWPFFooter footer = doc.createFooter(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.DEFAULT);
            XWPFParagraph footerPara = footer.createParagraph();
            XWPFRun footerRun = footerPara.createRun();
            footerRun.setText(content);

            try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                doc.write(fos);
            }
        }
        return file;
    }

    private String extractText(byte[] docxBytes) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText());
            }
            for (XWPFFooter f : doc.getFooterList()) {
                for (XWPFParagraph p : f.getParagraphs()) {
                    sb.append(p.getText());
                }
            }
            return sb.toString();
        }
    }
}