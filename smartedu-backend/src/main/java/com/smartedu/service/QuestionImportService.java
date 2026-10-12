package com.smartedu.service;

import com.smartedu.entity.Question;
import com.smartedu.mapper.QuestionMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

/**
 * 题目导入服务
 * 直接上传文件内容存入数据库
 * @author SmartEdu Team
 */
@Service
public class QuestionImportService {

    private final QuestionMapper questionMapper;

    public QuestionImportService(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @Transactional
    public java.util.Map<String, Object> importQuestions(MultipartFile file, Long courseId, Long userId) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        String content;

        // 解析文件内容
        if (extension.equals(".docx")) {
            content = parseDocx(file.getInputStream());
        } else if (extension.equals(".doc")) {
            content = parseDoc(file.getInputStream());
        } else if (extension.equals(".pdf")) {
            content = parsePdf(file.getInputStream());
        } else {
            throw new RuntimeException("仅支持 .doc .docx .pdf 格式");
        }

        if (content == null || content.trim().isEmpty()) {
            content = "文件内容为空";
        }

        // 创建一个题目，内容为整个文件内容
        Question question = new Question();
        question.setQuestionCode("Q-" + System.currentTimeMillis());
        question.setQuestionType("ESSAY");
        question.setContent(content.trim());
        question.setOptions("{}");
        question.setAnswer("");
        question.setAnalysis("");
        question.setDifficultyLevel(2);
        question.setScore(new BigDecimal("10.0"));
        question.setCourseId(courseId);
        question.setCreateUserId(userId);
        question.setStatus(1);

        questionMapper.insert(question);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("total", 1);
        result.put("success", 1);
        result.put("errors", new java.util.ArrayList<>());
        result.put("importedIds", java.util.List.of(question.getId()));

        return result;
    }

    private String parseDocx(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(is)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getText() != null) sb.append(p.getText()).append("\n");
            }
        }
        return sb.toString();
    }

    private String parseDoc(InputStream is) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(is);
             WordExtractor ex = new WordExtractor(doc)) {
            return ex.getText();
        }
    }

    private String parsePdf(InputStream is) throws IOException {
        try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
            return new PDFTextStripper().getText(doc);
        }
    }
}