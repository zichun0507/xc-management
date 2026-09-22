package com.xinchang.management.template.service;

import com.xinchang.management.common.BusinessException;
import com.xinchang.management.company.entity.Company;
import com.xinchang.management.employee.entity.Employee;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档渲染服务，负责将模板中的 #{字段名} 占位符替换为实际数据
 */
@Service
public class DocRenderService {

    private static final Logger log = LoggerFactory.getLogger(DocRenderService.class);
    private static final long TIMEOUT_MS = 5 * 60 * 1000;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("#\\{(\\w+)\\}");

    /**
     * 渲染模板文档，将占位符替换为实际字段值
     * @param templatePath 模板文件路径
     * @param fieldMap 字段名到值的映射
     * @return 渲染后的文档字节数组
     */
    public byte[] render(String templatePath, Map<String, String> fieldMap) {
        List<String> missingFields = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument doc = new XWPFDocument(fis)) {

            // 依次替换文档正文、表格、页眉、页脚中的占位符
            replaceInBody(doc, fieldMap, missingFields, startTime);
            replaceInTables(doc, fieldMap, missingFields, startTime);
            replaceInHeaders(doc, fieldMap, missingFields, startTime);
            replaceInFooters(doc, fieldMap, missingFields, startTime);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);

            if (!missingFields.isEmpty()) {
                log.warn("模板中缺少以下字段值: {}", String.join(", ", missingFields));
            }
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("模板异常：无法读取模板文件");
        }
    }

    private void replaceInBody(XWPFDocument doc, Map<String, String> fieldMap,
                                  List<String> missingFields, long startTime) {
        for (XWPFParagraph para : doc.getParagraphs()) {
            checkTimeout(startTime);
            replaceInParagraph(para, fieldMap, missingFields);
        }
    }

    private void replaceInTables(XWPFDocument doc, Map<String, String> fieldMap,
                                   List<String> missingFields, long startTime) {
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        checkTimeout(startTime);
                        replaceInParagraph(para, fieldMap, missingFields);
                    }
                }
            }
        }
    }

    private void replaceInHeaders(XWPFDocument doc, Map<String, String> fieldMap,
                                    List<String> missingFields, long startTime) {
        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph para : header.getParagraphs()) {
                checkTimeout(startTime);
                replaceInParagraph(para, fieldMap, missingFields);
            }
            for (XWPFTable table : header.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph para : cell.getParagraphs()) {
                            replaceInParagraph(para, fieldMap, missingFields);
                        }
                    }
                }
            }
        }
    }

    private void replaceInFooters(XWPFDocument doc, Map<String, String> fieldMap,
                                    List<String> missingFields, long startTime) {
        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph para : footer.getParagraphs()) {
                checkTimeout(startTime);
                replaceInParagraph(para, fieldMap, missingFields);
            }
            for (XWPFTable table : footer.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph para : cell.getParagraphs()) {
                            replaceInParagraph(para, fieldMap, missingFields);
                        }
                    }
                }
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph para, Map<String, String> fieldMap,
                                       List<String> missingFields) {
        if (!hasPlaceholder(para.getText())) return;

        List<XWPFRun> runs = para.getRuns();
        if (runs == null || runs.isEmpty()) return;

        boolean replaced = trySimpleRunReplacement(runs, fieldMap, missingFields);
        if (!replaced) {
            tryMergedReplacement(runs, fieldMap, missingFields);
        }
    }

    private boolean trySimpleRunReplacement(List<XWPFRun> runs, Map<String, String> fieldMap,
                                                List<String> missingFields) {
        boolean anyChanged = false;
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text == null) continue;
            String before = text;
            for (Map.Entry<String, String> e : fieldMap.entrySet()) {
                String placeholder = "#{" + e.getKey() + "}";
                if (text.contains(placeholder)) {
                    String value = e.getValue() != null ? e.getValue() : "";
                    text = text.replace(placeholder, value);
                    if (value.isEmpty()) missingFields.add(e.getKey());
                }
            }
            if (!text.equals(before)) {
                run.setText(text, 0);
                anyChanged = true;
            }
        }
        return anyChanged;
    }

    private void tryMergedReplacement(List<XWPFRun> runs, Map<String, String> fieldMap,
                                         List<String> missingFields) {
        StringBuilder merged = new StringBuilder();
        for (XWPFRun run : runs) {
            String t = run.getText(0);
            if (t != null) merged.append(t);
        }
        String original = merged.toString();
        String replaced = original;
        for (Map.Entry<String, String> e : fieldMap.entrySet()) {
            String placeholder = "#{" + e.getKey() + "}";
            if (replaced.contains(placeholder)) {
                String value = e.getValue() != null ? e.getValue() : "";
                replaced = replaced.replace(placeholder, value);
                if (value.isEmpty()) missingFields.add(e.getKey());
            }
        }
        if (!replaced.equals(original)) {
            runs.get(0).setText(replaced, 0);
            for (int i = 1; i < runs.size(); i++) {
                runs.get(i).setText("", 0);
            }
        }
    }

    private boolean hasPlaceholder(String text) {
        if (text == null) return false;
        return PLACEHOLDER_PATTERN.matcher(text).find();
    }

    private void checkTimeout(long startTime) {
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            throw new BusinessException("导出超时，请减少导出数量后重试");
        }
    }

    // ==================== Field Map Builders ====================

    /**
     * 构建企业字段映射Map，用于模板渲染替换
     * @param company 企业实体
     * @return 字段名到字段值的映射
     */
    public Map<String, String> buildCompanyFieldMap(Company company) {
        Map<String, String> map = new HashMap<>();
        map.put("companyName", company.getCompanyName());
        map.put("shortName", safe(company.getShortName()));
        map.put("englishName", safe(company.getEnglishName()));
        map.put("unifiedCode", safe(company.getUnifiedCode()));
        map.put("legalPerson", safe(company.getLegalPerson()));
        map.put("contactPerson", safe(company.getContactPerson()));
        map.put("contactPhone", safe(company.getContactPhone()));
        map.put("address", safe(company.getAddress()));
        map.put("leaseStartDate", company.getLeaseStartDate() != null
                ? company.getLeaseStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        map.put("leaseEndDate", company.getLeaseEndDate() != null
                ? company.getLeaseEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        return map;
    }

    /**
     * 构建人员字段映射Map，用于模板渲染替换
     * @param employee 人员实体
     * @return 字段名到字段值的映射
     */
    public Map<String, String> buildEmployeeFieldMap(Employee employee) {
        Map<String, String> map = new HashMap<>();
        map.put("name", safe(employee.getName()));
        map.put("gender", safe(employee.getGender()));
        map.put("idCard", safe(employee.getIdCard()));
        map.put("school", safe(employee.getSchool()));
        map.put("major", safe(employee.getMajor()));
        map.put("phone", safe(employee.getPhone()));
        map.put("position", safe(employee.getPosition()));
        return map;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
