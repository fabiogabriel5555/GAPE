package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;

public final class AssessmentView {

    private final Assessment assessment;
    private final String subjectName;
    private final String subjectAcronym;
    private final String contentBlockName;
    private final Integer contentBlockOrderNo;
    private final Long classGroupId;
    private final String classGroupCode;
    private final int questionCount;
    private final int attemptCount;

    private AssessmentView(
            Assessment assessment,
            String subjectName,
            String subjectAcronym,
            String contentBlockName,
            Integer contentBlockOrderNo,
            Long classGroupId,
            String classGroupCode,
            int questionCount,
            int attemptCount
    ) {
        this.assessment = assessment;
        this.subjectName = subjectName;
        this.subjectAcronym = subjectAcronym;
        this.contentBlockName = contentBlockName;
        this.contentBlockOrderNo = contentBlockOrderNo;
        this.classGroupId = classGroupId;
        this.classGroupCode = classGroupCode;
        this.questionCount = questionCount;
        this.attemptCount = attemptCount;
    }

    public static AssessmentView from(
            Assessment assessment,
            String subjectName,
            String subjectAcronym,
            String contentBlockName,
            Integer contentBlockOrderNo,
            Long classGroupId,
            String classGroupCode,
            int questionCount,
            int attemptCount
    ) {
        return new AssessmentView(
                assessment,
                subjectName,
                subjectAcronym,
                contentBlockName,
                contentBlockOrderNo,
                classGroupId,
                classGroupCode,
                questionCount,
                attemptCount
        );
    }

    public long getId() {
        return assessment.id();
    }

    public Long getSubjectId() {
        return assessment.subjectId();
    }

    public Long getContentBlockId() {
        return assessment.contentBlockId();
    }

    public String getTitle() {
        return assessment.title();
    }

    public String getDescription() {
        return assessment.description() == null || assessment.description().isBlank()
                ? "No description."
                : assessment.description();
    }

    public String getDescriptionValue() {
        return assessment.description() == null ? "" : assessment.description();
    }

    public String getType() {
        return assessment.type().name();
    }

    public String getTypeValue() {
        return assessment.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return switch (assessment.type()) {
            case FORM -> "Form";
            case TEST -> "Test";
            case EXAM -> "Exam";
        };
    }

    public boolean isForm() {
        return assessment.type() == AssessmentType.FORM;
    }

    public boolean isTest() {
        return assessment.type() == AssessmentType.TEST;
    }

    public boolean isExam() {
        return assessment.type() == AssessmentType.EXAM;
    }

    public String getMode() {
        return assessment.mode().name();
    }

    public String getModeValue() {
        return assessment.mode().toDatabaseValue();
    }

    public String getModeLabel() {
        return switch (assessment.mode()) {
            case ONLINE -> "Online";
            case ONSITE -> "In-Person";
        };
    }

    public String getPhysicalRoomCode() {
        return assessment.physicalRoomCode();
    }

    public boolean isHasRoom() {
        return assessment.physicalRoomCode() != null && !assessment.physicalRoomCode().isBlank();
    }

    public boolean isOnline() {
        return assessment.mode() == AssessmentMode.ONLINE;
    }

    public String getCorrectionMode() {
        return assessment.correctionMode().name();
    }

    public String getCorrectionModeValue() {
        return assessment.correctionMode().toDatabaseValue();
    }

    public String getCorrectionModeLabel() {
        return switch (assessment.correctionMode()) {
            case AUTOMATIC -> "Automatic";
            case MIXED -> "Mixed";
            case MANUAL -> "Manual";
        };
    }

    public boolean isAutomaticCorrectionAllowed() {
        return assessment.correctionMode().allowsAutomaticCorrection();
    }

    public boolean isManualCorrectionAllowed() {
        return assessment.correctionMode().allowsManualCorrection();
    }

    public boolean isManualOnly() {
        return assessment.correctionMode() == AssessmentCorrectionMode.MANUAL;
    }

    public BigDecimal getMaxGradeRaw() {
        return assessment.maxGrade();
    }

    public BigDecimal getPassingGradeRaw() {
        return assessment.passingGrade();
    }

    public BigDecimal getFinalGradeWeightRaw() {
        return assessment.finalGradeWeight();
    }

    public String getMaxGrade() {
        return gradeLabel(assessment.maxGrade());
    }

    public String getPassingGrade() {
        return gradeLabel(assessment.passingGrade());
    }

    public String getFinalGradeWeight() {
        return gradeLabel(assessment.finalGradeWeight());
    }

    public Integer getAttemptsLimit() {
        return assessment.attemptsLimit();
    }

    public String getAttemptsLimitLabel() {
        return assessment.attemptsLimit() == null ? "Unlimited" : assessment.attemptsLimit().toString();
    }

    public String getEnrollmentMode() {
        return assessment.enrollmentMode().name();
    }

    public String getEnrollmentModeValue() {
        return assessment.enrollmentMode().toDatabaseValue();
    }

    public String getEnrollmentModeLabel() {
        return switch (assessment.enrollmentMode()) {
            case AUTO_APPROVE -> "Automatic enrollment";
            case MANUAL -> "Manual enrollment";
        };
    }

    public boolean isAutomaticEnrollment() {
        return assessment.enrollmentMode() == pt.isel.gape.learning.model.EnrollmentApprovalMode.AUTO_APPROVE;
    }

    public String getState() {
        return assessment.state().name();
    }

    public String getStateValue() {
        return assessment.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (assessment.state()) {
            case DRAFT -> "Draft";
            case SCHEDULED -> "Scheduled";
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
        };
    }

    public String getStateBadgeClass() {
        return switch (assessment.state()) {
            case DRAFT -> "bg-neutral-30 text-neutral-600";
            case SCHEDULED -> "bg-info-50 text-info-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case COMPLETED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isActive() {
        return assessment.state() == AssessmentState.ACTIVE;
    }

    public boolean isCompleted() {
        return assessment.state() == AssessmentState.COMPLETED;
    }

    public String getAvailableFrom() {
        return displayDateTime(assessment.availableFrom());
    }

    public String getAvailableUntil() {
        return displayDateTime(assessment.availableUntil());
    }

    public LocalDateTime getAvailableFromRaw() {
        return assessment.availableFrom();
    }

    public LocalDateTime getAvailableUntilRaw() {
        return assessment.availableUntil();
    }

    public Integer getOrderNoRaw() {
        return assessment.orderNo();
    }

    public String getAvailableFromValue() {
        return inputDateTime(assessment.availableFrom());
    }

    public String getAvailableUntilValue() {
        return inputDateTime(assessment.availableUntil());
    }

    public String getAvailabilityLabel() {
        if (assessment.availableFrom() == null && assessment.availableUntil() == null) {
            return "Always available";
        }
        return (assessment.availableFrom() == null ? "-" : displayDateTime(assessment.availableFrom()))
                + " to "
                + (assessment.availableUntil() == null ? "-" : displayDateTime(assessment.availableUntil()));
    }

    public String getSubjectName() {
        return subjectName == null || subjectName.isBlank() ? "No subject" : subjectName;
    }

    public String getSubjectAcronym() {
        return subjectAcronym == null || subjectAcronym.isBlank() ? "-" : subjectAcronym;
    }

    public String getContentBlockName() {
        return contentBlockName == null || contentBlockName.isBlank() ? "" : contentBlockName;
    }

    public Integer getContentBlockOrderNo() {
        return contentBlockOrderNo;
    }

    public Long getClassGroupId() {
        return classGroupId;
    }

    public String getClassGroupCode() {
        return classGroupCode == null || classGroupCode.isBlank() ? "" : classGroupCode;
    }

    public boolean isBlockAssessment() {
        return assessment.contentBlockId() != null;
    }

    public String getContextLabel() {
        if (isBlockAssessment()) {
            return getClassGroupCode() + " | " + getContentBlockName();
        }
        return getSubjectName();
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getQuestionCountLabel() {
        return questionCount == 1 ? "1 question" : questionCount + " questions";
    }

    public String getAttemptCountLabel() {
        return attemptCount == 1 ? "1 attempt" : attemptCount + " attempts";
    }

    public String getIconClass() {
        return isExam() ? "ph ph-seal-check" : "ph ph-eye";
    }

    public String getSoftClass() {
        return switch (assessment.type()) {
            case EXAM -> "bg-warning-50 text-warning-600";
            case TEST -> "bg-info-50 text-info-600";
            case FORM -> "bg-main-50 text-main-600";
        };
    }

    private static String displayDateTime(LocalDateTime value) {
        return value == null ? "" : ApplicationDateTimeFormat.dateTime(value);
    }

    private static String inputDateTime(LocalDateTime value) {
        return value == null ? "" : ApplicationDateTimeFormat.TECHNICAL_DATE_TIME.format(value);
    }

    private static String gradeLabel(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }
}
