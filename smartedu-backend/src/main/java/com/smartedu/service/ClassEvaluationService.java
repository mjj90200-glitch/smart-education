package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.dto.ClassEvaluationSaveRequest;
import com.smartedu.entity.ClassEvaluationRecord;
import com.smartedu.entity.ClassEvaluationSession;
import com.smartedu.entity.Course;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.entity.User;
import com.smartedu.mapper.ClassEvaluationRecordMapper;
import com.smartedu.mapper.ClassEvaluationSessionMapper;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.mapper.HomeworkSubmissionMapper;
import com.smartedu.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClassEvaluationService {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final String STATUS_UNKNOWN = "UNMARKED";
    private static final String STATUS_PRESENT = "PRESENT";
    private static final String STATUS_LATE = "LATE";
    private static final String STATUS_ABSENT = "ABSENT";
    private static final String STATUS_LEAVE = "LEAVE";

    private final ClassEvaluationSessionMapper sessionMapper;
    private final ClassEvaluationRecordMapper recordMapper;
    private final CourseMapper courseMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    public ClassEvaluationService(ClassEvaluationSessionMapper sessionMapper,
                                  ClassEvaluationRecordMapper recordMapper,
                                  CourseMapper courseMapper,
                                  HomeworkMapper homeworkMapper,
                                  HomeworkSubmissionMapper submissionMapper,
                                  UserMapper userMapper,
                                  JdbcTemplate jdbcTemplate) {
        this.sessionMapper = sessionMapper;
        this.recordMapper = recordMapper;
        this.courseMapper = courseMapper;
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS class_evaluation_sessions (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                teacher_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                week_number INT NULL,
                session_date DATE NOT NULL,
                session_title VARCHAR(200) NULL,
                teaching_topic VARCHAR(200) NULL,
                source_file_name VARCHAR(255) NULL,
                start_time TIME NULL,
                end_time TIME NULL,
                total_students INT DEFAULT 0,
                present_count INT DEFAULT 0,
                absent_count INT DEFAULT 0,
                leave_count INT DEFAULT 0,
                late_count INT DEFAULT 0,
                answer_count INT DEFAULT 0,
                question_count INT DEFAULT 0,
                interaction_count INT DEFAULT 0,
                high_interaction_count INT DEFAULT 0,
                average_participation_score DECIMAL(5,2) DEFAULT 0,
                analysis_summary TEXT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted TINYINT DEFAULT 0,
                UNIQUE KEY uk_teacher_course_date (teacher_id, course_id, session_date),
                KEY idx_course_id (course_id),
                KEY idx_session_date (session_date),
                KEY idx_week_number (week_number)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂评估场次表'
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS class_evaluation_records (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                session_id BIGINT NOT NULL,
                student_id BIGINT NULL,
                student_no VARCHAR(64) NULL,
                student_name VARCHAR(100) NULL,
                class_name VARCHAR(100) NULL,
                performance_level VARCHAR(20) NULL,
                attendance_status VARCHAR(20) DEFAULT 'UNMARKED',
                answer_count INT DEFAULT 0,
                question_count INT DEFAULT 0,
                participation_score INT DEFAULT 0,
                teacher_comment VARCHAR(500) NULL,
                absent_reason VARCHAR(255) NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted TINYINT DEFAULT 0,
                UNIQUE KEY uk_session_student (session_id, student_id),
                KEY idx_session_id (session_id),
                KEY idx_student_id (student_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂评估学生记录表'
            """);

        ensureColumn("class_evaluation_sessions", "week_number",
            "ALTER TABLE class_evaluation_sessions ADD COLUMN week_number INT NULL AFTER course_id");
        ensureColumn("class_evaluation_sessions", "source_file_name",
            "ALTER TABLE class_evaluation_sessions ADD COLUMN source_file_name VARCHAR(255) NULL AFTER teaching_topic");
        ensureColumn("class_evaluation_records", "student_no",
            "ALTER TABLE class_evaluation_records ADD COLUMN student_no VARCHAR(64) NULL AFTER student_id");
        ensureColumn("class_evaluation_records", "student_name",
            "ALTER TABLE class_evaluation_records ADD COLUMN student_name VARCHAR(100) NULL AFTER student_no");
        ensureColumn("class_evaluation_records", "class_name",
            "ALTER TABLE class_evaluation_records ADD COLUMN class_name VARCHAR(100) NULL AFTER student_name");
        ensureColumn("class_evaluation_records", "performance_level",
            "ALTER TABLE class_evaluation_records ADD COLUMN performance_level VARCHAR(20) NULL AFTER class_name");
        jdbcTemplate.execute("ALTER TABLE class_evaluation_records MODIFY COLUMN student_id BIGINT NULL");
    }

    public Map<String, Object> getOverview(Long teacherId, Long courseId, LocalDate sessionDate, Integer weekNumber) {
        Course course = requireCourseAccess(teacherId, courseId);
        List<Integer> availableWeeks = sessionMapper.selectList(new LambdaQueryWrapper<ClassEvaluationSession>()
                .eq(ClassEvaluationSession::getTeacherId, teacherId)
                .eq(ClassEvaluationSession::getCourseId, courseId)
                .eq(ClassEvaluationSession::getDeleted, 0)
                .isNotNull(ClassEvaluationSession::getWeekNumber)
                .select(ClassEvaluationSession::getWeekNumber)
                .orderByAsc(ClassEvaluationSession::getWeekNumber))
            .stream()
            .map(ClassEvaluationSession::getWeekNumber)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        Integer resolvedWeekNumber = weekNumber;
        if (resolvedWeekNumber == null && !availableWeeks.isEmpty()) {
            resolvedWeekNumber = availableWeeks.get(0);
        }

        LocalDate targetDate = sessionDate == null ? LocalDate.now() : sessionDate;
        ClassEvaluationSession session = findSession(teacherId, courseId, targetDate, resolvedWeekNumber);
        if (session != null) {
            targetDate = session.getSessionDate();
        }

        List<User> students = findCourseStudents(course);
        Map<Long, User> studentMap = students.stream().collect(Collectors.toMap(User::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<Map<String, Object>> studentRows = buildStudentRows(session, studentMap);
        Map<String, Object> summary = buildSummary(studentRows);

        Map<String, Object> sessionInfo = new LinkedHashMap<>();
        sessionInfo.put("exists", session != null);
        sessionInfo.put("id", session == null ? null : session.getId());
        sessionInfo.put("weekNumber", session == null ? resolvedWeekNumber : session.getWeekNumber());
        sessionInfo.put("sessionDate", targetDate.toString());
        sessionInfo.put("sessionTitle", session != null && session.getSessionTitle() != null ? session.getSessionTitle() : course.getCourseName() + " 课堂评估");
        sessionInfo.put("teachingTopic", session == null ? null : session.getTeachingTopic());
        sessionInfo.put("sourceFileName", session == null ? null : session.getSourceFileName());
        sessionInfo.put("startTime", session == null || session.getStartTime() == null ? null : session.getStartTime().toString());
        sessionInfo.put("endTime", session == null || session.getEndTime() == null ? null : session.getEndTime().toString());
        sessionInfo.put("analysisSummary", session != null && session.getAnalysisSummary() != null
            ? session.getAnalysisSummary()
            : buildAnalysisSummary(summary, studentRows));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("course", buildCourseInfo(course));
        result.put("session", sessionInfo);
        result.put("summary", summary);
        result.put("students", studentRows);
        result.put("insights", buildInsights(studentRows));
        result.put("availableWeeks", availableWeeks);
        return result;
    }

    @Transactional
    public Map<String, Object> saveSession(Long teacherId, ClassEvaluationSaveRequest request) {
        if (request.getCourseId() == null) {
            throw new BusinessException("课程不能为空");
        }
        if (request.getSessionDate() == null) {
            throw new BusinessException("上课日期不能为空");
        }

        Course course = requireCourseAccess(teacherId, request.getCourseId());
        List<User> students = findCourseStudents(course);
        Map<Long, User> studentMap = students.stream().collect(Collectors.toMap(User::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<ClassEvaluationSaveRequest.StudentRecord> incomingRows = request.getStudents() == null
            ? Collections.emptyList()
            : request.getStudents();
        if (incomingRows.isEmpty()) {
            throw new BusinessException("请至少填写一条学生课堂记录");
        }

        ClassEvaluationSession session = findSession(teacherId, request.getCourseId(), request.getSessionDate(), request.getWeekNumber());

        if (session == null) {
            session = new ClassEvaluationSession();
            session.setTeacherId(teacherId);
            session.setCourseId(request.getCourseId());
            session.setSessionDate(request.getSessionDate());
            session.setWeekNumber(request.getWeekNumber());
            session.setDeleted(0);
            session.setCreatedAt(LocalDateTime.now());
        }

        List<Map<String, Object>> normalizedRows = normalizeRows(incomingRows, studentMap);
        Map<String, Object> summary = buildSummary(normalizedRows);

        session.setSessionTitle(blankToNull(request.getSessionTitle()) == null ? course.getCourseName() + " 课堂评估" : request.getSessionTitle().trim());
        session.setTeachingTopic(blankToNull(request.getTeachingTopic()));
        session.setWeekNumber(request.getWeekNumber());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setTotalStudents((Integer) summary.get("studentCount"));
        session.setPresentCount((Integer) summary.get("presentCount"));
        session.setAbsentCount((Integer) summary.get("absentCount"));
        session.setLeaveCount((Integer) summary.get("leaveCount"));
        session.setLateCount((Integer) summary.get("lateCount"));
        session.setAnswerCount((Integer) summary.get("totalAnswerCount"));
        session.setQuestionCount((Integer) summary.get("totalQuestionCount"));
        session.setInteractionCount((Integer) summary.get("totalInteractionCount"));
        session.setHighInteractionCount((Integer) summary.get("highInteractionCount"));
        session.setAverageParticipationScore(new BigDecimal(String.valueOf(summary.get("averageParticipationScore"))));
        session.setAnalysisSummary(blankToNull(request.getAnalysisSummary()) == null ? buildAnalysisSummary(summary, normalizedRows) : request.getAnalysisSummary().trim());
        session.setUpdatedAt(LocalDateTime.now());

        if (session.getId() == null) {
            sessionMapper.insert(session);
        } else {
            sessionMapper.updateById(session);
        }

        recordMapper.delete(new LambdaQueryWrapper<ClassEvaluationRecord>()
            .eq(ClassEvaluationRecord::getSessionId, session.getId()));

        for (Map<String, Object> row : normalizedRows) {
            ClassEvaluationRecord record = new ClassEvaluationRecord();
            record.setSessionId(session.getId());
            record.setStudentId(row.get("studentId") == null ? null : ((Number) row.get("studentId")).longValue());
            record.setStudentNo((String) row.get("studentNo"));
            record.setStudentName((String) row.get("studentName"));
            record.setClassName((String) row.get("className"));
            record.setPerformanceLevel((String) row.get("performanceLevel"));
            record.setAttendanceStatus(String.valueOf(row.get("attendanceStatus")));
            record.setAnswerCount((Integer) row.get("answerCount"));
            record.setQuestionCount((Integer) row.get("questionCount"));
            record.setParticipationScore((Integer) row.get("participationScore"));
            record.setTeacherComment((String) row.get("teacherComment"));
            record.setAbsentReason((String) row.get("absentReason"));
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.setDeleted(0);
            recordMapper.insert(record);
        }

        return getOverview(teacherId, request.getCourseId(), request.getSessionDate(), request.getWeekNumber());
    }

    @Transactional
    public Map<String, Object> importTemplate(Long teacherId, Long courseId, LocalDate termStartDate, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }
        Course course = requireCourseAccess(teacherId, courseId);

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new BusinessException("Excel 内容为空");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("缺少表头");
            }

            LocalDate baseDate = termStartDate == null ? LocalDate.now().withDayOfMonth(1) : termStartDate;
            Map<Integer, ClassEvaluationSession> sessionByWeek = new LinkedHashMap<>();
            Map<Integer, List<ClassEvaluationRecord>> recordsByWeek = new LinkedHashMap<>();
            Map<String, User> usersByName = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "STUDENT")
                    .eq(User::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(User::getRealName, item -> item, (a, b) -> a));

            int maxCol = headerRow.getLastCellNum();
            for (int col = 2; col < maxCol; col++) {
                Integer week = parseWeekNumber(cellText(headerRow.getCell(col)));
                if (week == null) {
                    continue;
                }
                LocalDate sessionDate = baseDate.plusWeeks(Math.max(week - 1, 0));
                ClassEvaluationSession session = findSession(teacherId, courseId, sessionDate, week);
                if (session == null) {
                    session = new ClassEvaluationSession();
                    session.setTeacherId(teacherId);
                    session.setCourseId(courseId);
                    session.setWeekNumber(week);
                    session.setSessionDate(sessionDate);
                    session.setDeleted(0);
                    session.setCreatedAt(LocalDateTime.now());
                }
                session.setSessionTitle(course.getCourseName() + " 第" + week + "周课堂评估");
                session.setTeachingTopic("Excel 导入");
                session.setSourceFileName(file.getOriginalFilename());
                session.setUpdatedAt(LocalDateTime.now());
                sessionByWeek.put(week, session);
                recordsByWeek.put(week, new ArrayList<>());
            }

            if (sessionByWeek.isEmpty()) {
                throw new BusinessException("未识别到周次表头，请使用标准模板");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String studentNo = cellText(row.getCell(0));
                String studentName = cellText(row.getCell(1));
                if (blankToNull(studentNo) == null && blankToNull(studentName) == null) {
                    continue;
                }

                User matchedUser = blankToNull(studentName) == null ? null : usersByName.get(studentName);
                String className = matchedUser == null ? "" : valueOrEmpty(matchedUser.getClassName());

                for (Integer week : sessionByWeek.keySet()) {
                    int col = week + 1;
                    String level = cellText(row.getCell(col));
                    if (blankToNull(level) == null) {
                        continue;
                    }
                    recordsByWeek.get(week).add(createImportedRecord(matchedUser, studentNo, studentName, className, level));
                }
            }

            Integer firstWeek = null;
            for (Map.Entry<Integer, ClassEvaluationSession> entry : sessionByWeek.entrySet()) {
                Integer week = entry.getKey();
                List<ClassEvaluationRecord> records = recordsByWeek.getOrDefault(week, Collections.emptyList());
                if (records.isEmpty()) {
                    continue;
                }

                ClassEvaluationSession session = entry.getValue();
                List<Map<String, Object>> rowMaps = records.stream().map(this::toRowMap).collect(Collectors.toList());
                Map<String, Object> summary = buildSummary(rowMaps);
                session.setTotalStudents((Integer) summary.get("studentCount"));
                session.setPresentCount((Integer) summary.get("presentCount"));
                session.setAbsentCount((Integer) summary.get("absentCount"));
                session.setLeaveCount((Integer) summary.get("leaveCount"));
                session.setLateCount((Integer) summary.get("lateCount"));
                session.setAnswerCount((Integer) summary.get("totalAnswerCount"));
                session.setQuestionCount((Integer) summary.get("totalQuestionCount"));
                session.setInteractionCount((Integer) summary.get("totalInteractionCount"));
                session.setHighInteractionCount((Integer) summary.get("highInteractionCount"));
                session.setAverageParticipationScore(new BigDecimal(String.valueOf(summary.get("averageParticipationScore"))));
                session.setAnalysisSummary(buildAnalysisSummary(summary, rowMaps));

                if (session.getId() == null) {
                    sessionMapper.insert(session);
                } else {
                    sessionMapper.updateById(session);
                    recordMapper.delete(new LambdaQueryWrapper<ClassEvaluationRecord>()
                        .eq(ClassEvaluationRecord::getSessionId, session.getId()));
                }

                for (ClassEvaluationRecord record : records) {
                    record.setSessionId(session.getId());
                    record.setCreatedAt(LocalDateTime.now());
                    record.setUpdatedAt(LocalDateTime.now());
                    record.setDeleted(0);
                    recordMapper.insert(record);
                }

                if (firstWeek == null) {
                    firstWeek = week;
                }
            }

            if (firstWeek == null) {
                throw new BusinessException("模板中没有可导入的学生课堂数据");
            }

            Map<String, Object> result = getOverview(teacherId, courseId, sessionByWeek.get(firstWeek).getSessionDate(), firstWeek);
            result.put("importSummary", Map.of(
                "fileName", file.getOriginalFilename(),
                "importedWeeks", new ArrayList<>(sessionByWeek.keySet()),
                "studentCount", recordsByWeek.values().stream().flatMap(List::stream).map(ClassEvaluationRecord::getStudentName).filter(Objects::nonNull).distinct().count()
            ));
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析课堂评估 Excel 失败", e);
        }
    }

    public void exportExcel(Long teacherId, Long courseId, LocalDate sessionDate, Integer weekNumber, HttpServletResponse response) {
        Map<String, Object> overview = getOverview(teacherId, courseId, sessionDate, weekNumber);
        Map<String, Object> course = (Map<String, Object>) overview.get("course");
        Map<String, Object> session = (Map<String, Object>) overview.get("session");
        Map<String, Object> summary = (Map<String, Object>) overview.get("summary");
        List<Map<String, Object>> students = (List<Map<String, Object>>) overview.get("students");

        try (Workbook workbook = new XSSFWorkbook()) {
            createSummarySheet(workbook, course, session, summary);
            createStudentSheet(workbook, students);

            String suffix = session.get("weekNumber") == null
                ? String.valueOf(session.get("sessionDate"))
                : "week-" + session.get("weekNumber");
            String fileName = String.format(Locale.ROOT, "class-evaluation-%s-%s.xlsx",
                sanitizeFileName(String.valueOf(course.get("courseName"))),
                suffix);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException("导出课堂评估 Excel 失败", e);
        }
    }

    private void createSummarySheet(Workbook workbook, Map<String, Object> course, Map<String, Object> session, Map<String, Object> summary) {
        Sheet sheet = workbook.createSheet("课堂概览");
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 28 * 256);

        org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setAlignment(HorizontalAlignment.LEFT);

        int rowIndex = 0;
        rowIndex = writeKeyValue(sheet, rowIndex, "课程", String.valueOf(course.get("courseName")));
        rowIndex = writeKeyValue(sheet, rowIndex, "日期", String.valueOf(session.get("sessionDate")));
        rowIndex = writeKeyValue(sheet, rowIndex, "周次", session.get("weekNumber") == null ? "-" : "第" + session.get("weekNumber") + "周");
        rowIndex = writeKeyValue(sheet, rowIndex, "课堂标题", String.valueOf(session.get("sessionTitle")));
        rowIndex = writeKeyValue(sheet, rowIndex, "授课主题", valueOrDash(session.get("teachingTopic")));
        rowIndex = writeKeyValue(sheet, rowIndex, "上课时间", joinTime(session.get("startTime"), session.get("endTime")));
        rowIndex++;
        rowIndex = writeKeyValue(sheet, rowIndex, "应到人数", String.valueOf(summary.get("studentCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "到课人数", String.valueOf(summary.get("presentCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "迟到人数", String.valueOf(summary.get("lateCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "缺勤人数", String.valueOf(summary.get("absentCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "请假人数", String.valueOf(summary.get("leaveCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "到课率", String.valueOf(summary.get("attendanceRate")) + "%");
        rowIndex = writeKeyValue(sheet, rowIndex, "答题总次数", String.valueOf(summary.get("totalAnswerCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "提问总次数", String.valueOf(summary.get("totalQuestionCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "互动总次数", String.valueOf(summary.get("totalInteractionCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "高互动学生数", String.valueOf(summary.get("highInteractionCount")));
        rowIndex = writeKeyValue(sheet, rowIndex, "平均课堂参与分", String.valueOf(summary.get("averageParticipationScore")));
        writeKeyValue(sheet, rowIndex, "课堂分析", valueOrDash(session.get("analysisSummary")));
    }

    private void createStudentSheet(Workbook workbook, List<Map<String, Object>> students) {
        Sheet sheet = workbook.createSheet("学生明细");
        String[] headers = {"姓名", "班级", "模板标记", "出勤状态", "答题次数", "提问次数", "互动总数", "参与分", "缺勤原因", "教师备注"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            sheet.setColumnWidth(i, (i == 9 ? 24 : 16) * 256);
        }

        int rowIndex = 1;
        for (Map<String, Object> item : students) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(String.valueOf(item.get("studentName")));
            row.createCell(1).setCellValue(valueOrDash(item.get("className")));
            row.createCell(2).setCellValue(valueOrDash(item.get("performanceLevel")));
            row.createCell(3).setCellValue(attendanceLabel(String.valueOf(item.get("attendanceStatus"))));
            row.createCell(4).setCellValue(((Number) item.get("answerCount")).intValue());
            row.createCell(5).setCellValue(((Number) item.get("questionCount")).intValue());
            row.createCell(6).setCellValue(((Number) item.get("interactionCount")).intValue());
            row.createCell(7).setCellValue(((Number) item.get("participationScore")).intValue());
            row.createCell(8).setCellValue(valueOrDash(item.get("absentReason")));
            row.createCell(9).setCellValue(valueOrDash(item.get("teacherComment")));
        }
    }

    private int writeKeyValue(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);
        return rowIndex + 1;
    }

    private Map<String, Object> buildCourseInfo(Course course) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", course.getId());
        result.put("courseName", course.getCourseName());
        result.put("courseCode", course.getCourseCode());
        result.put("semester", course.getSemester());
        return result;
    }

    private List<Map<String, Object>> buildStudentRows(ClassEvaluationSession session, Map<Long, User> studentMap) {
        if (session != null) {
            List<ClassEvaluationRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ClassEvaluationRecord>()
                .eq(ClassEvaluationRecord::getSessionId, session.getId())
                .eq(ClassEvaluationRecord::getDeleted, 0)
                .orderByAsc(ClassEvaluationRecord::getStudentName)
                .orderByAsc(ClassEvaluationRecord::getId));
            if (!records.isEmpty()) {
                return records.stream().map(this::toRowMap).collect(Collectors.toList());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (User student : studentMap.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("studentNo", "");
            row.put("studentName", student.getRealName());
            row.put("className", student.getClassName());
            row.put("performanceLevel", "");
            row.put("attendanceStatus", STATUS_UNKNOWN);
            row.put("answerCount", 0);
            row.put("questionCount", 0);
            row.put("interactionCount", (Integer) row.get("answerCount") + (Integer) row.get("questionCount"));
            row.put("participationScore", 0);
            row.put("teacherComment", "");
            row.put("absentReason", "");
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> normalizeRows(List<ClassEvaluationSaveRequest.StudentRecord> incomingRows, Map<Long, User> studentMap) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (ClassEvaluationSaveRequest.StudentRecord input : incomingRows) {
            User student = input.getStudentId() == null ? null : studentMap.get(input.getStudentId());
            int answerCount = safeNonNegative(input.getAnswerCount());
            int questionCount = safeNonNegative(input.getQuestionCount());
            int participationScore = Math.min(100, safeNonNegative(input.getParticipationScore()));
            String attendanceStatus = normalizeStatus(input.getAttendanceStatus());
            String studentName = student == null ? valueOrEmpty(input.getStudentName()) : student.getRealName();
            String key = (input.getStudentId() == null ? "name:" + studentName : "id:" + input.getStudentId());
            seenKeys.add(key);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student == null ? input.getStudentId() : student.getId());
            row.put("studentNo", valueOrEmpty(input.getStudentNo()));
            row.put("studentName", studentName);
            row.put("className", student == null ? valueOrEmpty(input.getClassName()) : valueOrEmpty(student.getClassName()));
            row.put("performanceLevel", valueOrEmpty(input.getPerformanceLevel()));
            row.put("attendanceStatus", attendanceStatus);
            row.put("answerCount", answerCount);
            row.put("questionCount", questionCount);
            row.put("interactionCount", answerCount + questionCount);
            row.put("participationScore", participationScore);
            row.put("teacherComment", valueOrEmpty(input.getTeacherComment()));
            row.put("absentReason", valueOrEmpty(input.getAbsentReason()));
            rows.add(row);
        }

        for (User student : studentMap.values()) {
            if (seenKeys.contains("id:" + student.getId())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("studentNo", "");
            row.put("studentName", student.getRealName());
            row.put("className", student.getClassName());
            row.put("performanceLevel", "");
            row.put("attendanceStatus", STATUS_UNKNOWN);
            row.put("answerCount", 0);
            row.put("questionCount", 0);
            row.put("interactionCount", 0);
            row.put("participationScore", 0);
            row.put("teacherComment", "");
            row.put("absentReason", "");
            rows.add(row);
        }

        rows.sort(Comparator.comparing(item -> String.valueOf(item.get("studentName"))));
        return rows;
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> rows) {
        int studentCount = rows.size();
        int presentCount = countByStatus(rows, STATUS_PRESENT);
        int lateCount = countByStatus(rows, STATUS_LATE);
        int absentCount = countByStatus(rows, STATUS_ABSENT);
        int leaveCount = countByStatus(rows, STATUS_LEAVE);
        int totalAnswerCount = rows.stream().mapToInt(item -> (Integer) item.get("answerCount")).sum();
        int totalQuestionCount = rows.stream().mapToInt(item -> (Integer) item.get("questionCount")).sum();
        int totalInteractionCount = rows.stream().mapToInt(item -> (Integer) item.get("interactionCount")).sum();
        int highInteractionCount = (int) rows.stream()
            .filter(item -> ((Integer) item.get("interactionCount")) >= 3 || ((Integer) item.get("participationScore")) >= 80)
            .count();
        BigDecimal attendanceRate = studentCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf((presentCount + lateCount) * 100.0 / studentCount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageParticipationScore = studentCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(rows.stream().mapToInt(item -> (Integer) item.get("participationScore")).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("studentCount", studentCount);
        summary.put("presentCount", presentCount);
        summary.put("lateCount", lateCount);
        summary.put("absentCount", absentCount);
        summary.put("leaveCount", leaveCount);
        summary.put("attendanceRate", attendanceRate);
        summary.put("totalAnswerCount", totalAnswerCount);
        summary.put("totalQuestionCount", totalQuestionCount);
        summary.put("totalInteractionCount", totalInteractionCount);
        summary.put("highInteractionCount", highInteractionCount);
        summary.put("averageParticipationScore", averageParticipationScore);
        return summary;
    }

    private Map<String, Object> buildInsights(List<Map<String, Object>> rows) {
        List<Map<String, Object>> topParticipants = rows.stream()
            .sorted(Comparator
                .comparing((Map<String, Object> item) -> (Integer) item.get("participationScore")).reversed()
                .thenComparing(item -> (Integer) item.get("interactionCount"), Comparator.reverseOrder()))
            .limit(5)
            .collect(Collectors.toList());

        List<Map<String, Object>> absentStudents = rows.stream()
            .filter(item -> STATUS_ABSENT.equals(item.get("attendanceStatus")) || STATUS_LEAVE.equals(item.get("attendanceStatus")))
            .collect(Collectors.toList());

        List<Map<String, Object>> lowParticipationStudents = rows.stream()
            .filter(item -> !STATUS_ABSENT.equals(item.get("attendanceStatus")))
            .filter(item -> !STATUS_LEAVE.equals(item.get("attendanceStatus")))
            .filter(item -> ((Integer) item.get("interactionCount")) == 0 && ((Integer) item.get("participationScore")) < 60)
            .collect(Collectors.toList());

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("topParticipants", topParticipants);
        insights.put("absentStudents", absentStudents);
        insights.put("lowParticipationStudents", lowParticipationStudents);
        return insights;
    }

    private String buildAnalysisSummary(Map<String, Object> summary, List<Map<String, Object>> rows) {
        BigDecimal attendanceRate = (BigDecimal) summary.get("attendanceRate");
        BigDecimal avgScore = (BigDecimal) summary.get("averageParticipationScore");
        int interactions = (Integer) summary.get("totalInteractionCount");
        int absentCount = (Integer) summary.get("absentCount");
        int leaveCount = (Integer) summary.get("leaveCount");

        StringBuilder builder = new StringBuilder();
        builder.append("本节课到课率 ").append(attendanceRate).append("%");
        if (attendanceRate.compareTo(new BigDecimal("90")) >= 0) {
            builder.append("，整体出勤表现稳定。");
        } else if (attendanceRate.compareTo(new BigDecimal("75")) >= 0) {
            builder.append("，出勤情况基本正常，但仍需关注少数缺勤学生。");
        } else {
            builder.append("，缺勤情况较明显，建议及时跟进。");
        }

        builder.append(" 课堂总互动 ").append(interactions).append(" 次，平均参与分 ").append(avgScore).append("。");
        if (avgScore.compareTo(new BigDecimal("80")) >= 0) {
            builder.append(" 学生整体参与度较高。");
        } else if (avgScore.compareTo(new BigDecimal("60")) >= 0) {
            builder.append(" 课堂参与度中等，可以增加点名提问或分组互动。");
        } else {
            builder.append(" 课堂参与度偏低，建议优化提问节奏和互动设计。");
        }

        if (absentCount + leaveCount > 0) {
            List<String> names = rows.stream()
                .filter(item -> STATUS_ABSENT.equals(item.get("attendanceStatus")) || STATUS_LEAVE.equals(item.get("attendanceStatus")))
                .map(item -> String.valueOf(item.get("studentName")))
                .limit(5)
                .collect(Collectors.toList());
            builder.append(" 需重点关注缺勤/请假学生：").append(String.join("、", names)).append("。");
        }

        return builder.toString();
    }

    private Map<String, Object> toRowMap(ClassEvaluationRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("studentId", record.getStudentId());
        row.put("studentNo", valueOrEmpty(record.getStudentNo()));
        row.put("studentName", blankToNull(record.getStudentName()) == null ? "未命名学生" : record.getStudentName());
        row.put("className", valueOrEmpty(record.getClassName()));
        row.put("performanceLevel", valueOrEmpty(record.getPerformanceLevel()));
        row.put("attendanceStatus", normalizeStatus(record.getAttendanceStatus()));
        row.put("answerCount", record.getAnswerCount() == null ? 0 : record.getAnswerCount());
        row.put("questionCount", record.getQuestionCount() == null ? 0 : record.getQuestionCount());
        row.put("interactionCount", (record.getAnswerCount() == null ? 0 : record.getAnswerCount())
            + (record.getQuestionCount() == null ? 0 : record.getQuestionCount()));
        row.put("participationScore", record.getParticipationScore() == null ? 0 : record.getParticipationScore());
        row.put("teacherComment", valueOrEmpty(record.getTeacherComment()));
        row.put("absentReason", valueOrEmpty(record.getAbsentReason()));
        return row;
    }

    private ClassEvaluationRecord createImportedRecord(User matchedUser, String studentNo, String studentName, String className, String level) {
        ClassEvaluationRecord record = new ClassEvaluationRecord();
        record.setStudentId(matchedUser == null ? null : matchedUser.getId());
        record.setStudentNo(studentNo);
        record.setStudentName(studentName);
        record.setClassName(className);
        record.setPerformanceLevel(level);

        switch (level.trim()) {
            case "优" -> {
                record.setAttendanceStatus(STATUS_PRESENT);
                record.setAnswerCount(3);
                record.setQuestionCount(1);
                record.setParticipationScore(95);
                record.setTeacherComment("课堂表现优秀");
            }
            case "良" -> {
                record.setAttendanceStatus(STATUS_PRESENT);
                record.setAnswerCount(2);
                record.setQuestionCount(1);
                record.setParticipationScore(82);
                record.setTeacherComment("课堂参与积极");
            }
            case "及" -> {
                record.setAttendanceStatus(STATUS_PRESENT);
                record.setAnswerCount(1);
                record.setQuestionCount(0);
                record.setParticipationScore(68);
                record.setTeacherComment("课堂表现达到基本要求");
            }
            case "缺" -> {
                record.setAttendanceStatus(STATUS_ABSENT);
                record.setAnswerCount(0);
                record.setQuestionCount(0);
                record.setParticipationScore(0);
                record.setTeacherComment("Excel 导入：缺勤");
                record.setAbsentReason("缺勤");
            }
            case "假" -> {
                record.setAttendanceStatus(STATUS_LEAVE);
                record.setAnswerCount(0);
                record.setQuestionCount(0);
                record.setParticipationScore(0);
                record.setTeacherComment("Excel 导入：请假");
                record.setAbsentReason("请假");
            }
            default -> {
                record.setAttendanceStatus(STATUS_UNKNOWN);
                record.setAnswerCount(0);
                record.setQuestionCount(0);
                record.setParticipationScore(0);
                record.setTeacherComment("Excel 导入：未识别标记");
            }
        }
        return record;
    }

    private ClassEvaluationSession findSession(Long teacherId, Long courseId, LocalDate sessionDate, Integer weekNumber) {
        LambdaQueryWrapper<ClassEvaluationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassEvaluationSession::getTeacherId, teacherId);
        wrapper.eq(ClassEvaluationSession::getCourseId, courseId);
        wrapper.eq(ClassEvaluationSession::getDeleted, 0);
        if (weekNumber != null) {
            wrapper.eq(ClassEvaluationSession::getWeekNumber, weekNumber);
        } else {
            wrapper.eq(ClassEvaluationSession::getSessionDate, sessionDate);
        }
        wrapper.last("LIMIT 1");
        return sessionMapper.selectOne(wrapper);
    }

    private Integer parseWeekNumber(String text) {
        if (blankToNull(text) == null) {
            return null;
        }
        String normalized = text.replace("第", "").replace("周", "").trim();
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String cellText(Cell cell) {
        return cell == null ? "" : DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class,
            tableName,
            columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private Course requireCourseAccess(Long teacherId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (Objects.equals(course.getTeacherId(), teacherId)) {
            return course;
        }
        Long homeworkCount = homeworkMapper.selectCount(new LambdaQueryWrapper<Homework>()
            .eq(Homework::getTeacherId, teacherId)
            .eq(Homework::getCourseId, courseId));
        if (homeworkCount == null || homeworkCount == 0) {
            throw new BusinessException("无权查看该课程课堂评估");
        }
        return course;
    }

    private List<User> findCourseStudents(Course course) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getRole, "STUDENT");
        queryWrapper.eq(User::getStatus, 1);
        queryWrapper.eq(course.getGrade() != null && !course.getGrade().isBlank(), User::getGrade, course.getGrade());
        queryWrapper.eq(course.getMajor() != null && !course.getMajor().isBlank(), User::getMajor, course.getMajor());
        queryWrapper.orderByAsc(User::getRealName);
        List<User> matchedStudents = userMapper.selectList(queryWrapper);

        LinkedHashMap<Long, User> mergedStudents = new LinkedHashMap<>();
        for (User student : matchedStudents) {
            mergedStudents.put(student.getId(), student);
        }

        List<Homework> courseHomeworks = homeworkMapper.selectList(new LambdaQueryWrapper<Homework>()
            .eq(Homework::getCourseId, course.getId())
            .select(Homework::getId));

        List<Long> homeworkIds = courseHomeworks.stream()
            .map(Homework::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (!homeworkIds.isEmpty()) {
            List<HomeworkSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<HomeworkSubmission>()
                .in(HomeworkSubmission::getHomeworkId, homeworkIds)
                .select(HomeworkSubmission::getUserId));
            for (HomeworkSubmission submission : submissions) {
                if (submission.getUserId() == null || mergedStudents.containsKey(submission.getUserId())) {
                    continue;
                }
                User student = userMapper.selectById(submission.getUserId());
                if (student != null && "STUDENT".equals(student.getRole()) && Objects.equals(student.getStatus(), 1)) {
                    mergedStudents.put(student.getId(), student);
                }
            }
        }

        return new ArrayList<>(mergedStudents.values());
    }

    private int countByStatus(List<Map<String, Object>> rows, String status) {
        return (int) rows.stream().filter(item -> status.equals(item.get("attendanceStatus"))).count();
    }

    private int safeNonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private String normalizeStatus(String status) {
        if (blankToNull(status) == null) {
            return STATUS_UNKNOWN;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case STATUS_PRESENT -> STATUS_PRESENT;
            case STATUS_LATE -> STATUS_LATE;
            case STATUS_ABSENT -> STATUS_ABSENT;
            case STATUS_LEAVE -> STATUS_LEAVE;
            default -> STATUS_UNKNOWN;
        };
    }

    private String attendanceLabel(String status) {
        return switch (normalizeStatus(status)) {
            case STATUS_PRESENT -> "到课";
            case STATUS_LATE -> "迟到";
            case STATUS_ABSENT -> "缺勤";
            case STATUS_LEAVE -> "请假";
            default -> "未标记";
        };
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String valueOrDash(Object value) {
        return blankToNull(value == null ? null : String.valueOf(value)) == null ? "-" : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private String joinTime(Object startTime, Object endTime) {
        String start = blankToNull(startTime == null ? null : String.valueOf(startTime));
        String end = blankToNull(endTime == null ? null : String.valueOf(endTime));
        if (start == null && end == null) {
            return "-";
        }
        return (start == null ? "-" : start) + " - " + (end == null ? "-" : end);
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
    }
}
