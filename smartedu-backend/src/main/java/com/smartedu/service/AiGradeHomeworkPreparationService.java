package com.smartedu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiGradeHomeworkPreparationService {

    public static final String ANSWER_KEY_MARKER = "AI_GRADE_OBJECTIVE_V1";

    private static final Pattern QUESTION_START_PATTERN = Pattern.compile(
        "^\\s*(?:第\\s*\\d+\\s*[题、.．)]|\\d+\\s*[、.．)])"
    );
    private static final Pattern ANSWER_LABEL_PATTERN = Pattern.compile(
        "(?:参考答案|正确答案|答案|Answer)\\s*[】\\]]?\\s*[:：]\\s*[\"“”']?",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern OPTION_PATTERN = Pattern.compile("(?is).*[A-D]\\s*[、.．)]\\s*.+[B-D]\\s*[、.．)]\\s*.*");
    private static final Pattern INLINE_OPTION_START_PATTERN = Pattern.compile("^\\s*[A-Da-d]\\s*[、.．)]\\s*.*");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public PreparedHomeworkPackage prepare(MultipartFile sourceFile, String title) throws IOException {
        String sourceText = extractText(sourceFile);
        List<QuestionAnswer> answers = parseQuestionAnswers(sourceText);
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("没有识别到题目答案。请确认上传文件只包含选择题或填空题，并在每道题末尾写明“答案：”。");
        }

        Path generatedFile = writeStudentHomeworkDocx(answers, title);
        String relativeUrl = toUploadUrl(generatedFile);

        PreparedHomeworkPackage result = new PreparedHomeworkPackage();
        result.attachmentUrl = relativeUrl;
        result.attachmentName = safeBaseName(title, sourceFile.getOriginalFilename()) + "-AI批改版.docx";
        result.answerKeyContent = buildAnswerKey(answers);
        result.questionCount = answers.size();
        return result;
    }

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        byte[] bytes = file.getBytes();

        if (filename.endsWith(".docx")) {
            try (InputStream input = new ByteArrayInputStream(bytes);
                 XWPFDocument document = new XWPFDocument(input);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }

        if (filename.endsWith(".doc")) {
            try (InputStream input = new ByteArrayInputStream(bytes);
                 HWPFDocument document = new HWPFDocument(input);
                 WordExtractor extractor = new WordExtractor(document)) {
                return extractor.getText();
            }
        }

        if (filename.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(document);
            }
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<QuestionAnswer> parseQuestionAnswers(String sourceText) {
        String normalizedText = normalizeSourceText(sourceText);
        String[] lines = normalizedText.split("\\n");
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                if (current.length() > 0) {
                    current.append('\n');
                }
                continue;
            }

            if (QUESTION_START_PATTERN.matcher(trimmed).find() && current.length() > 0) {
                blocks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }

        if (current.length() > 0) {
            blocks.add(current.toString().trim());
        }

        if (blocks.isEmpty()) {
            blocks.add(normalizedText.trim());
        }

        List<QuestionAnswer> answers = new ArrayList<>();
        List<Integer> missingAnswerNumbers = new ArrayList<>();
        int nextNumber = 1;
        for (String block : blocks) {
            if (block.isBlank() || !looksLikeQuestion(block)) {
                continue;
            }

            AnswerExtraction extraction = extractAnswerAndStudentText(block);
            if (!extraction.hasAnswerLabel) {
                missingAnswerNumbers.add(nextNumber);
                nextNumber++;
                continue;
            }

            QuestionAnswer item = new QuestionAnswer();
            item.number = answers.size() + 1;
            item.type = OPTION_PATTERN.matcher(extraction.studentText).matches() ? "choice" : "fill_blank";
            item.question = formatQuestionText(extraction.studentText);
            item.answer = extraction.answer;
            answers.add(item);
            nextNumber++;
        }

        if (!answers.isEmpty() && !missingAnswerNumbers.isEmpty()) {
            throw new IllegalArgumentException("有题目没有识别到答案，请在每道题末尾补充“答案：”。缺少答案的题号：" + missingAnswerNumbers);
        }

        return answers;
    }

    private String normalizeSourceText(String sourceText) {
        String text = sourceText == null ? "" : sourceText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00A0', ' ');

        text = text.replaceAll("(?m)^\\s*[•·]\\s*", "");
        text = text.replaceAll("\\s+(?=\\d+\\s*[、.．)]\\s*[^\\d])", "\n");
        text = text.replaceAll("\\s+(?=[A-Da-d]\\s*[、.．)]\\s*)", "\n");
        text = text.replaceAll("(?m)^\\s*(\\d+)\\s*$\\n\\s*", "$1. ");
        return text;
    }

    private boolean looksLikeQuestion(String block) {
        String trimmed = block.trim();
        return QUESTION_START_PATTERN.matcher(trimmed).find() || ANSWER_LABEL_PATTERN.matcher(trimmed).find();
    }

    private AnswerExtraction extractAnswerAndStudentText(String block) {
        Matcher matcher = ANSWER_LABEL_PATTERN.matcher(block);
        if (!matcher.find()) {
            AnswerExtraction extraction = new AnswerExtraction();
            extraction.studentText = block.trim();
            extraction.answer = "";
            extraction.hasAnswerLabel = false;
            return extraction;
        }

        String before = block.substring(0, matcher.start());
        String after = block.substring(matcher.end());
        int lineEnd = after.indexOf('\n');
        String inlineTail = lineEnd >= 0 ? after.substring(0, lineEnd) : after;
        String remainingAfterLine = lineEnd >= 0 ? after.substring(lineEnd) : "";
        String candidate = cleanupAnswer(inlineTail);

        boolean hasInlineAnswer = !candidate.isBlank()
            && !INLINE_OPTION_START_PATTERN.matcher(candidate).matches()
            && !candidate.matches("^[\"“”'\\s]*$");

        String studentText;
        String answer;
        if (hasInlineAnswer) {
            answer = candidate;
            studentText = (before + remainingAfterLine).trim();
        } else {
            answer = "";
            studentText = (before + after).trim();
        }

        AnswerExtraction extraction = new AnswerExtraction();
        extraction.studentText = studentText
            .replaceAll("(?m)^\\s*(?:参考答案|正确答案|答案|Answer)\\s*[】\\]]?\\s*[:：].*$", "")
            .trim();
        extraction.answer = answer;
        extraction.hasAnswerLabel = true;
        return extraction;
    }

    private String cleanupAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer
            .replaceAll("^[\"“”'\\s]+|[\"“”'\\s]+$", "")
            .replaceAll("[。；;，,]$", "")
            .trim();
    }

    private String formatQuestionText(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replaceAll("\\s+(?=[A-Da-d]\\s*[、.．)]\\s*)", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    private Path writeStudentHomeworkDocx(List<QuestionAnswer> answers, String title) throws IOException {
        String projectDir = System.getProperty("user.dir");
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path dirPath = Paths.get(projectDir, uploadDir, "homework", datePath);
        Files.createDirectories(dirPath);

        Path filePath = dirPath.resolve(UUID.randomUUID() + ".docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(filePath)) {
            addParagraph(document, title == null || title.isBlank() ? "AI 批改作业" : title, true);
            addParagraph(document, "说明：本作业仅支持选择题或填空题。请在每道题下方“学生作答：”后填写答案。", false);
            addParagraph(document, "", false);

            for (QuestionAnswer answer : answers) {
                addTextBlock(document, answer.question);
                addParagraph(document, "学生作答：____________________", false);
                addParagraph(document, "", false);
            }

            document.write(output);
        }
        return filePath;
    }

    private void addParagraph(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setText(text == null ? "" : text);
    }

    private void addTextBlock(XWPFDocument document, String text) {
        String[] lines = (text == null ? "" : text).split("\\n");
        for (String line : lines) {
            addParagraph(document, line, false);
        }
    }

    private String buildAnswerKey(List<QuestionAnswer> answers) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QuestionAnswer answer : answers) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("number", answer.number);
            row.put("type", answer.type);
            row.put("answer", answer.answer);
            row.put("question", answer.question);
            rows.add(row);
        }
        return ANSWER_KEY_MARKER + "\n" + objectMapper.writeValueAsString(rows);
    }

    private String toUploadUrl(Path filePath) {
        String normalized = filePath.toString().replace('\\', '/');
        int index = normalized.indexOf("/" + uploadDir + "/");
        if (index >= 0) {
            return normalized.substring(index);
        }
        return "/" + uploadDir + "/" + filePath.getFileName();
    }

    private String safeBaseName(String title, String originalFilename) {
        String raw = title != null && !title.isBlank() ? title : originalFilename;
        if (raw == null || raw.isBlank()) {
            return "作业";
        }
        int dotIndex = raw.lastIndexOf('.');
        if (dotIndex > 0) {
            raw = raw.substring(0, dotIndex);
        }
        return raw.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static class QuestionAnswer {
        int number;
        String type;
        String question;
        String answer;
    }

    private static class AnswerExtraction {
        String studentText;
        String answer;
        boolean hasAnswerLabel;
    }

    public static class PreparedHomeworkPackage {
        public String attachmentUrl;
        public String attachmentName;
        public String answerKeyContent;
        public int questionCount;
    }
}
