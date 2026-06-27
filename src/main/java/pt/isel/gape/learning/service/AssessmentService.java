package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AssessmentEnrollmentDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ContentAssociationDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.ContentItemDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentCreateCommand;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;
import pt.isel.gape.learning.model.AssessmentUpdateCommand;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.ContentAssociationCommand;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemCreateCommand;
import pt.isel.gape.learning.model.ContentItemState;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionCreateCommand;
import pt.isel.gape.learning.model.QuestionState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class AssessmentService {

    private static final int TITLE_MAX_LENGTH = 160;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final AssessmentEnrollmentDAO assessmentEnrollmentDAO;
    private final SubjectDAO subjectDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final ClassGroupDAO classGroupDAO;
    private final ContentItemDAO contentItemDAO;
    private final ContentAssociationDAO contentAssociationDAO;
    private final QuestionDAO questionDAO;
    private final QuestionOptionDAO optionDAO;
    private final AssessmentAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public AssessmentService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new AssessmentEnrollmentDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new ContentBlockDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new ContentItemDAO(connectionProvider),
                new ContentAssociationDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new QuestionOptionDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public AssessmentService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            AssessmentEnrollmentDAO assessmentEnrollmentDAO,
            SubjectDAO subjectDAO,
            ContentBlockDAO contentBlockDAO,
            ClassGroupDAO classGroupDAO,
            ContentItemDAO contentItemDAO,
            ContentAssociationDAO contentAssociationDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.assessmentEnrollmentDAO = Objects.requireNonNull(
                assessmentEnrollmentDAO,
                "assessmentEnrollmentDAO is required"
        );
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.contentBlockDAO = Objects.requireNonNull(contentBlockDAO, "contentBlockDAO is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.contentItemDAO = Objects.requireNonNull(contentItemDAO, "contentItemDAO is required");
        this.contentAssociationDAO = Objects.requireNonNull(contentAssociationDAO, "contentAssociationDAO is required");
        this.questionDAO = Objects.requireNonNull(questionDAO, "questionDAO is required");
        this.optionDAO = Objects.requireNonNull(optionDAO, "optionDAO is required");
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new AssessmentAccessPolicy(
                assessmentDAO,
                permissionDAO,
                new PermissionChecker(
                        permissionDAO,
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                )
        );
        this.auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
    }

    public Assessment createAssessment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            AssessmentCreateCommand command,
            String sourceIp
    ) {
        try {
            LocalDateTime now = currentMinute();
            AssessmentCreateCommand effectiveCommand = deriveTemporalState(command, now);
            validateCreateCommand(effectiveCommand, now);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection);
                    NormalizedCreateAssessment context = normalizeCreateContext(connection, effectiveCommand);
                    requireContextManagers(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            context.context(),
                            null,
                            sourceIp
                    );
                    long assessmentId = assessmentDAO.create(connection, context.command());
                    assessmentDAO.replaceApplicableClassGroups(
                            connection,
                            assessmentId,
                            context.context().persistedClassGroupIds()
                    );
                    Assessment assessment = requireAssessment(connection, assessmentId);
                    synchronizeAssessmentRepositoryReference(connection, actorUserId, assessment);
                    synchronizeAutomaticEnrollments(connection, assessment);
                    synchronizeTemporalStates(connection);
                    auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_CREATE",
                            "assessment", Long.toString(assessmentId), "success", sourceIp);
                    assessment = requireAssessment(connection, assessmentId);
                    connection.commit();
                    return assessment;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create assessment");
        }
    }

    public Assessment getAssessment(long assessmentId) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection);
                return requireAssessment(connection, assessmentId);
            }
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read assessment");
        }
    }

    public boolean canManageAssessment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            synchronizeTemporalStates(connection);
            Assessment assessment = assessmentDAO.findById(connection, assessmentId)
                    .orElse(null);
            if (assessment == null) {
                return false;
            }
            accessPolicy.requireAssessmentManager(
                    connection,
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    assessment,
                    sourceIp
            );
            return true;
        } catch (RuntimeException | SQLException exception) {
            return false;
        }
    }

    public boolean canManageContext(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Long subjectId,
            Long classGroupId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            synchronizeTemporalStates(connection);
            accessPolicy.requireContextManager(
                    connection,
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    subjectId,
                    classGroupId,
                    null,
                    sourceIp
            );
            return true;
        } catch (RuntimeException | SQLException exception) {
            return false;
        }
    }

    public Assessment updateAssessment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            AssessmentUpdateCommand command,
            String sourceIp
    ) {
        try {
            LocalDateTime now = currentMinute();
            AssessmentUpdateCommand effectiveCommand = deriveTemporalState(command, now);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection);
                    Assessment current = assessmentDAO.lockById(connection, assessmentId)
                            .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
                    ensureAssessmentCanBeUpdated(current);
                    validateUpdateCommand(effectiveCommand, now);
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            current,
                            sourceIp
                    );
                    NormalizedUpdateAssessment context = normalizeUpdateContext(connection, effectiveCommand, current);
                    requireContextManagers(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            context.context(),
                            assessmentId,
                            sourceIp
                    );
                    List<Long> currentClassGroupIds = assessmentDAO.findApplicableClassGroupIds(connection, assessmentId);
                    if (assessmentDAO.hasAnyAttempts(connection, assessmentId)
                            && structuralAssessmentChanged(
                                    current,
                                    context.command(),
                                    currentClassGroupIds,
                                    context.context().persistedClassGroupIds()
                            )) {
                        throw new IllegalStateException("Assessment structure cannot change after attempts have started");
                    }
                    requireQuestionScoresWithinMaximum(connection, assessmentId, context.command().maxGrade());
                    requireCorrectionModeCompatibleWithQuestions(
                            connection,
                            assessmentId,
                            context.command().correctionMode()
                    );
                    assessmentDAO.update(connection, assessmentId, context.command());
                    assessmentDAO.replaceApplicableClassGroups(
                            connection,
                            assessmentId,
                            context.context().persistedClassGroupIds()
                    );
                    Assessment updated = requireAssessment(connection, assessmentId);
                    synchronizeAssessmentRepositoryReference(connection, actorUserId, updated);
                    synchronizeAutomaticEnrollments(connection, updated);
                    synchronizeTemporalStates(connection);
                    auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_UPDATE",
                            "assessment", Long.toString(assessmentId), "success", sourceIp);
                    updated = requireAssessment(connection, assessmentId);
                    connection.commit();
                    return updated;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_UPDATE", Long.toString(assessmentId), sourceIp);
            throw wrap(exception, "Failed to update assessment");
        }
    }

    public Assessment archiveAssessment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection);
                    Assessment assessment = assessmentDAO.lockById(connection, assessmentId)
                            .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    assessmentDAO.updateState(connection, assessmentId, AssessmentState.COMPLETED);
                    auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_ARCHIVE",
                            "assessment", Long.toString(assessmentId), "success", sourceIp);
                    Assessment completed = requireAssessment(connection, assessmentId);
                    connection.commit();
                    return completed;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_ARCHIVE", Long.toString(assessmentId), sourceIp);
            throw wrap(exception, "Failed to archive assessment");
        }
    }

    public Assessment cloneAssessmentToBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long sourceAssessmentId,
            long targetContentBlockId,
            String sourceIp
    ) {
        try {
            LocalDateTime now = currentMinute();
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection);
                    Assessment source = assessmentDAO.findById(connection, sourceAssessmentId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + sourceAssessmentId));
                    AssessmentCreateCommand effectiveCommand = deriveTemporalState(
                            cloneCreateCommand(source, targetContentBlockId, now),
                            now
                    );
                    validateCreateCommand(effectiveCommand, now);
                    NormalizedCreateAssessment context = normalizeCreateContext(connection, effectiveCommand);
                    requireContextManagers(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            context.context(),
                            null,
                            sourceIp
                    );

                    long cloneId = assessmentDAO.create(connection, context.command());
                    assessmentDAO.replaceApplicableClassGroups(
                            connection,
                            cloneId,
                            context.context().persistedClassGroupIds()
                    );
                    for (Question question : questionDAO.findByAssessment(connection, sourceAssessmentId)) {
                        long cloneQuestionId = questionDAO.create(connection, new QuestionCreateCommand(
                                cloneId,
                                question.code(),
                                question.statement(),
                                question.type(),
                                question.orderNo(),
                                question.required(),
                                question.score(),
                                question.expectedAnswer(),
                                question.state()
                        ));
                        for (QuestionOption option : optionDAO.findByQuestion(connection, question.id())) {
                            optionDAO.create(connection, new QuestionOptionCreateCommand(
                                    cloneQuestionId,
                                    option.orderNo(),
                                    option.text(),
                                    option.correct(),
                                    option.state()
                            ));
                        }
                    }

                    Assessment clone = requireAssessment(connection, cloneId);
                    synchronizeAssessmentRepositoryReference(connection, actorUserId, clone);
                    synchronizeAutomaticEnrollments(connection, clone);
                    synchronizeTemporalStates(connection);
                    auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_REUSE",
                            "assessment", sourceAssessmentId + ":" + cloneId, "success", sourceIp);
                    clone = requireAssessment(connection, cloneId);
                    connection.commit();
                    return clone;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_REUSE", Long.toString(sourceAssessmentId), sourceIp);
            throw wrap(exception, "Failed to reuse assessment");
        }
    }

    public void deleteAssessment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Assessment assessment = assessmentDAO.lockById(connection, assessmentId)
                            .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    if (assessmentDAO.hasCertificateDependency(connection, assessmentId)) {
                        throw new IllegalStateException("Assessment cannot be deleted because certificates depend on it");
                    }
                    if (assessmentDAO.hasGradeSheetDependency(connection, assessmentId)) {
                        throw new IllegalStateException("Assessment cannot be deleted because grade sheets depend on it");
                    }
                    if (assessmentDAO.hasAnyAttempts(connection, assessmentId)) {
                        throw new IllegalStateException("Assessment cannot be deleted after attempts have been created");
                    }
                    contentItemDAO.findFirstItemBySource(connection, assessmentRepositorySource(assessmentId))
                            .ifPresent(contentItem -> {
                                try {
                                    contentItemDAO.updateState(
                                            connection,
                                            contentItem.id(),
                                            ContentItemState.INACTIVE,
                                            LocalDateTime.now(clock)
                                    );
                                } catch (SQLException exception) {
                                    throw new IllegalStateException("Failed to inactivate assessment repository item", exception);
                                }
                            });
                    assessmentDAO.delete(connection, assessmentId);
                    auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_DELETE",
                            "assessment", Long.toString(assessmentId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_DELETE", Long.toString(assessmentId), sourceIp);
            throw wrap(exception, "Failed to delete assessment");
        }
    }

    private NormalizedCreateAssessment normalizeCreateContext(
            Connection connection,
            AssessmentCreateCommand command
    ) throws SQLException {
        NormalizedContext context = resolveContext(
                connection,
                command.type(),
                command.subjectId(),
                command.contentBlockId(),
                command.classGroupIds()
        );
        return new NormalizedCreateAssessment(new AssessmentCreateCommand(
                context.subjectId(),
                context.contentBlockId(),
                command.title().trim(),
                normalizeText(command.description()),
                command.type(),
                command.mode(),
                command.correctionMode(),
                command.maxGrade(),
                command.passingGrade(),
                command.attemptsLimit(),
                command.enrollmentMode(),
                command.state(),
                command.availableFrom(),
                command.availableUntil(),
                context.persistedClassGroupIds()
        ), context);
    }

    private static AssessmentCreateCommand cloneCreateCommand(
            Assessment source,
            long targetContentBlockId,
            LocalDateTime now
    ) {
        LocalDateTime availableFrom = source.availableFrom();
        LocalDateTime availableUntil = source.availableUntil();
        if (availableUntil != null && !availableUntil.isAfter(now)) {
            availableFrom = null;
            availableUntil = null;
        } else if (availableFrom != null && availableFrom.isBefore(now)) {
            availableFrom = now;
        }
        return new AssessmentCreateCommand(
                null,
                targetContentBlockId,
                source.title(),
                source.description(),
                source.type(),
                source.mode(),
                source.correctionMode(),
                source.maxGrade(),
                source.passingGrade(),
                source.attemptsLimit(),
                source.enrollmentMode(),
                source.state(),
                availableFrom,
                availableUntil
        );
    }

    private NormalizedUpdateAssessment normalizeUpdateContext(
            Connection connection,
            AssessmentUpdateCommand command,
            Assessment current
    ) throws SQLException {
        List<Long> requestedClassGroupIds = command.classGroupIds();
        if (command.type() == AssessmentType.EXAM
                && command.contentBlockId() == null
                && requestedClassGroupIds.isEmpty()
                && current.type() == AssessmentType.EXAM
                && current.contentBlockId() == null) {
            requestedClassGroupIds = assessmentDAO.findApplicableClassGroupIds(connection, current.id());
        }
        NormalizedContext context = resolveContext(
                connection,
                command.type(),
                command.subjectId(),
                command.contentBlockId(),
                requestedClassGroupIds
        );
        return new NormalizedUpdateAssessment(new AssessmentUpdateCommand(
                context.subjectId(),
                context.contentBlockId(),
                command.title().trim(),
                normalizeText(command.description()),
                command.type(),
                command.mode(),
                command.correctionMode(),
                command.maxGrade(),
                command.passingGrade(),
                command.attemptsLimit(),
                command.enrollmentMode(),
                command.state(),
                command.availableFrom(),
                command.availableUntil(),
                context.persistedClassGroupIds()
        ), context);
    }

    private NormalizedContext resolveContext(
            Connection connection,
            AssessmentType type,
            Long subjectId,
            Long contentBlockId,
            List<Long> classGroupIds
    ) throws SQLException {
        if ((type == AssessmentType.FORM || type == AssessmentType.TEST) && contentBlockId == null) {
            throw new IllegalArgumentException("Form and test assessments require a content block");
        }
        if (type == AssessmentType.EXAM && subjectId == null && contentBlockId == null) {
            throw new IllegalArgumentException("Exam assessment requires a subject or content block");
        }

        Long resolvedSubjectId = subjectId;
        Long classGroupId = null;
        List<Long> requestedClassGroupIds = normalizeClassGroupIds(classGroupIds);
        if (contentBlockId != null) {
            ContentBlock contentBlock = contentBlockDAO.findById(connection, contentBlockId)
                    .orElseThrow(() -> new IllegalArgumentException("Content block not found: " + contentBlockId));
            if (contentBlock.state() == ContentBlockState.INACTIVE) {
                throw new IllegalStateException("Inactive content blocks cannot receive assessments");
            }
            ClassGroup classGroup = classGroupDAO.findById(connection, contentBlock.classGroupId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Class group not found: " + contentBlock.classGroupId()));
            if (classGroup.state() == ClassGroupState.DRAFT) {
                throw new IllegalStateException("Draft class groups cannot receive assessments");
            }
            classGroupId = classGroup.id();
            if (resolvedSubjectId == null) {
                resolvedSubjectId = classGroup.subjectId();
            } else if (!resolvedSubjectId.equals(classGroup.subjectId())) {
                throw new IllegalArgumentException("Assessment subject must match the content block subject");
            }
        }

        if (resolvedSubjectId != null) {
            Long subjectIdToLoad = resolvedSubjectId;
            Subject subject = subjectDAO.findById(connection, subjectIdToLoad)
                    .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectIdToLoad));
            if (subject.state() != SubjectState.ACTIVE) {
                throw new IllegalStateException("Assessments require an active subject");
            }
        }
        if (type == AssessmentType.EXAM && contentBlockId == null) {
            if (requestedClassGroupIds.isEmpty()) {
                throw new IllegalArgumentException("Subject-level exams require at least one applicable class group");
            }
            if (resolvedSubjectId == null) {
                throw new IllegalArgumentException("Subject-level exams require a subject");
            }
            if (!assessmentDAO.classGroupsMatchSubject(connection, resolvedSubjectId, requestedClassGroupIds)) {
                throw new IllegalArgumentException("Exam class groups must be active and belong to the assessment subject");
            }
            return new NormalizedContext(
                    resolvedSubjectId,
                    contentBlockId,
                    requestedClassGroupIds,
                    requestedClassGroupIds
            );
        }
        List<Long> accessClassGroupIds = classGroupId == null ? List.of() : List.of(classGroupId);
        return new NormalizedContext(resolvedSubjectId, contentBlockId, accessClassGroupIds, List.of());
    }

    private Assessment requireAssessment(Connection connection, long assessmentId) throws SQLException {
        return assessmentDAO.findById(connection, assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
    }

    private static boolean structuralAssessmentChanged(
            Assessment current,
            AssessmentUpdateCommand command,
            List<Long> currentClassGroupIds,
            List<Long> newClassGroupIds
    ) {
        return !Objects.equals(current.subjectId(), command.subjectId())
                || !Objects.equals(current.contentBlockId(), command.contentBlockId())
                || !Objects.equals(currentClassGroupIds, newClassGroupIds)
                || current.type() != command.type()
                || current.mode() != command.mode()
                || current.correctionMode() != command.correctionMode()
                || current.maxGrade().compareTo(command.maxGrade()) != 0
                || !Objects.equals(current.attemptsLimit(), command.attemptsLimit());
    }

    private void requireQuestionScoresWithinMaximum(
            Connection connection,
            long assessmentId,
            BigDecimal maxGrade
    ) throws SQLException {
        BigDecimal activeQuestionTotal = questionDAO.sumActiveScores(connection, assessmentId, null);
        if (activeQuestionTotal.compareTo(maxGrade) > 0) {
            throw new IllegalArgumentException("Active question scores cannot exceed assessment maximum grade");
        }
    }

    private void requireCorrectionModeCompatibleWithQuestions(
            Connection connection,
            long assessmentId,
            AssessmentCorrectionMode correctionMode
    ) throws SQLException {
        if (!correctionMode.requiresObjectiveOnly()) {
            return;
        }
        boolean hasManualQuestions = questionDAO.findByAssessment(connection, assessmentId).stream()
                .anyMatch(question -> question.state() == QuestionState.ACTIVE
                        && question.type().requiresManualScoring());
        if (hasManualQuestions) {
            throw new IllegalArgumentException("Automatic assessments can only contain objective active questions");
        }
    }

    private static void ensureAssessmentCanBeUpdated(Assessment assessment) {
        if (assessment.state() == AssessmentState.COMPLETED) {
            throw new IllegalStateException("Completed assessments cannot be changed");
        }
    }

    private void synchronizeAssessmentRepositoryReference(
            Connection connection,
            long actorUserId,
            Assessment assessment
    ) throws SQLException {
        String source = assessmentRepositorySource(assessment.id());
        ContentItem contentItem = contentItemDAO.findFirstItemBySource(connection, source).orElse(null);
        ContentItemCreateCommand reference = new ContentItemCreateCommand(
                assessment.title(),
                assessment.description(),
                ContentFormat.OTHER,
                source,
                assessment.state() == AssessmentState.COMPLETED
                        ? ContentItemState.INACTIVE
                        : ContentItemState.ACTIVE
        );
        long contentItemId;
        if (contentItem == null) {
            contentItemId = contentItemDAO.create(connection, actorUserId, reference, LocalDateTime.now(clock));
        } else {
            contentItemId = contentItem.id();
            contentItemDAO.update(
                    connection,
                    contentItemId,
                    new pt.isel.gape.learning.model.ContentItemUpdateCommand(
                            reference.title(),
                            reference.description(),
                            reference.format(),
                            reference.source(),
                            reference.state()
                    ),
                    LocalDateTime.now(clock)
            );
        }
        if (contentAssociationDAO
                .findAssociation(connection, ContentAssociationType.ASSESSMENT, assessment.id(), contentItemId)
                .isEmpty()) {
            contentAssociationDAO.associate(
                    connection,
                    contentItemId,
                    new ContentAssociationCommand(
                            ContentAssociationType.ASSESSMENT,
                            assessment.id(),
                            "assessment",
                            null,
                            false
                    )
            );
        }
    }

    private void synchronizeAutomaticEnrollments(Connection connection, Assessment assessment) throws SQLException {
        if (assessment.enrollmentMode() == EnrollmentApprovalMode.AUTO_APPROVE) {
            assessmentEnrollmentDAO.syncAutomaticEnrollments(
                    connection,
                    assessment.id(),
                    java.time.LocalDate.now(clock)
            );
        }
    }

    private LocalDateTime currentMinute() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    private static void validateCreateCommand(AssessmentCreateCommand command, LocalDateTime now) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.title(),
                command.description(),
                command.type(),
                command.mode(),
                command.correctionMode(),
                command.maxGrade(),
                command.passingGrade(),
                command.attemptsLimit(),
                command.enrollmentMode(),
                command.state(),
                command.availableFrom(),
                command.availableUntil(),
                now,
                true
        );
    }

    private static void validateUpdateCommand(AssessmentUpdateCommand command, LocalDateTime now) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.title(),
                command.description(),
                command.type(),
                command.mode(),
                command.correctionMode(),
                command.maxGrade(),
                command.passingGrade(),
                command.attemptsLimit(),
                command.enrollmentMode(),
                command.state(),
                command.availableFrom(),
                command.availableUntil(),
                now,
                false
        );
    }

    private static AssessmentCreateCommand deriveTemporalState(
            AssessmentCreateCommand command,
            LocalDateTime now
    ) {
        Objects.requireNonNull(command, "command is required");
        return new AssessmentCreateCommand(
                command.subjectId(),
                command.contentBlockId(),
                command.title(),
                command.description(),
                command.type(),
                command.mode(),
                command.correctionMode(),
                command.maxGrade(),
                command.passingGrade(),
                command.attemptsLimit(),
                command.enrollmentMode(),
                deriveTemporalState(command.availableFrom(), command.availableUntil(), now),
                command.availableFrom(),
                command.availableUntil(),
                command.classGroupIds()
        );
    }

    private static AssessmentUpdateCommand deriveTemporalState(
            AssessmentUpdateCommand command,
            LocalDateTime now
    ) {
        Objects.requireNonNull(command, "command is required");
        return new AssessmentUpdateCommand(
                command.subjectId(),
                command.contentBlockId(),
                command.title(),
                command.description(),
                command.type(),
                command.mode(),
                command.correctionMode(),
                command.maxGrade(),
                command.passingGrade(),
                command.attemptsLimit(),
                command.enrollmentMode(),
                deriveTemporalState(command.availableFrom(), command.availableUntil(), now),
                command.availableFrom(),
                command.availableUntil(),
                command.classGroupIds()
        );
    }

    private static AssessmentState deriveTemporalState(
            LocalDateTime availableFrom,
            LocalDateTime availableUntil,
            LocalDateTime now
    ) {
        if (availableFrom == null) {
            return AssessmentState.DRAFT;
        }
        if (availableUntil != null && !availableUntil.isAfter(now)) {
            return AssessmentState.COMPLETED;
        }
        if (!availableFrom.isAfter(now)) {
            return AssessmentState.ACTIVE;
        }
        return AssessmentState.SCHEDULED;
    }

    private static void validateCommonCommand(
            String title,
            String description,
            AssessmentType type,
            Object mode,
            Object correctionMode,
            BigDecimal maxGrade,
            BigDecimal passingGrade,
            Integer attemptsLimit,
            EnrollmentApprovalMode enrollmentMode,
            AssessmentState state,
            java.time.LocalDateTime availableFrom,
            java.time.LocalDateTime availableUntil,
            LocalDateTime now,
            boolean requireFutureDates
    ) {
        AcademicTextValidator.requireName(title, "Assessment title is required");
        requireMaxLength(title.trim(), TITLE_MAX_LENGTH, "Assessment title is too long");
        requireMaxLength(description, DESCRIPTION_MAX_LENGTH, "Assessment description is too long");
        Objects.requireNonNull(type, "assessment type is required");
        Objects.requireNonNull(mode, "assessment mode is required");
        Objects.requireNonNull(correctionMode, "assessment correction mode is required");
        Objects.requireNonNull(enrollmentMode, "assessment enrollment mode is required");
        Objects.requireNonNull(state, "assessment state is required");
        if (mode == AssessmentMode.ONSITE && correctionMode != AssessmentCorrectionMode.MANUAL) {
            throw new IllegalArgumentException("In-person assessments must use manual correction");
        }
        requirePositive(maxGrade, "Assessment maximum grade must be positive");
        requireNonNegative(passingGrade, "Assessment passing grade cannot be negative");
        if (passingGrade.compareTo(maxGrade) > 0) {
            throw new IllegalArgumentException("Assessment passing grade cannot exceed maximum grade");
        }
        if (attemptsLimit != null && attemptsLimit <= 0) {
            throw new IllegalArgumentException("Assessment attempts limit must be greater than zero");
        }
        requireValidDates(availableFrom, availableUntil, now, requireFutureDates);
    }

    private void requireContextManagers(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            NormalizedContext context,
            Long assessmentId,
            String sourceIp
    ) throws SQLException {
        if (!context.classGroupIds().isEmpty()) {
            for (Long classGroupId : context.classGroupIds()) {
                accessPolicy.requireContextManager(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        context.subjectId(),
                        classGroupId,
                        assessmentId,
                        sourceIp
                );
            }
            return;
        }
        accessPolicy.requireContextManager(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                context.subjectId(),
                null,
                assessmentId,
                sourceIp
        );
    }

    public int synchronizeTemporalStates() {
        try (Connection connection = connectionProvider.getConnection()) {
            return synchronizeTemporalStates(connection);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to synchronize assessment states");
        }
    }

    private int synchronizeTemporalStates(Connection connection) throws SQLException {
        LocalDateTime now = currentMinute();
        return assessmentDAO.synchronizeTemporalStates(connection, now);
    }

    private static void requireValidDates(
            LocalDateTime availableFrom,
            LocalDateTime availableUntil,
            LocalDateTime now,
            boolean requireFutureDates
    ) {
        Objects.requireNonNull(now, "now is required");
        if (availableUntil != null && availableFrom == null) {
            throw new IllegalArgumentException("Assessment availability end requires an availability start");
        }
        if (requireFutureDates && availableFrom != null && availableFrom.isBefore(now)) {
            throw new IllegalArgumentException("Assessment availability start cannot be in the past");
        }
        if (requireFutureDates && availableUntil != null && availableUntil.isBefore(now)) {
            throw new IllegalArgumentException("Assessment availability end cannot be in the past");
        }
        if (availableFrom != null && availableUntil != null && availableUntil.isBefore(availableFrom)) {
            throw new IllegalArgumentException("Assessment availability end cannot be before start");
        }
    }

    private static void requirePositive(BigDecimal value, String message) {
        Objects.requireNonNull(value, message);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonNegative(BigDecimal value, String message) {
        Objects.requireNonNull(value, message);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String assessmentRepositorySource(long assessmentId) {
        return "assessment:" + assessmentId;
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "assessment", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }

    private static List<Long> normalizeClassGroupIds(List<Long> classGroupIds) {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return List.of();
        }
        java.util.LinkedHashSet<Long> normalized = new java.util.LinkedHashSet<>();
        for (Long classGroupId : classGroupIds) {
            if (classGroupId == null || classGroupId <= 0) {
                throw new IllegalArgumentException("Class group ids must be positive");
            }
            normalized.add(classGroupId);
        }
        return List.copyOf(normalized);
    }

    private record NormalizedContext(
            Long subjectId,
            Long contentBlockId,
            List<Long> classGroupIds,
            List<Long> persistedClassGroupIds
    ) {
    }

    private record NormalizedCreateAssessment(AssessmentCreateCommand command, NormalizedContext context) {
    }

    private record NormalizedUpdateAssessment(AssessmentUpdateCommand command, NormalizedContext context) {
    }
}
