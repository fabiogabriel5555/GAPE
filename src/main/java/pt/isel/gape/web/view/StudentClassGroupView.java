package pt.isel.gape.web.view;

import java.util.List;
import java.util.Map;

public final class StudentClassGroupView {

    private final ClassGroupView classGroup;
    private final ClassGroupEnrollmentView enrollment;
    private final List<ContentBlockView> contentBlocks;
    private final Map<Long, List<BlockContentItemView>> blockContentsByBlock;
    private final Map<Long, List<LessonView>> blockLessonsByBlock;
    private final Map<Long, List<AssessmentView>> blockAssessmentsByBlock;
    private final Map<Long, List<BlockActivityView>> blockActivitiesByBlock;
    private final boolean eligibleForEnrollment;

    private StudentClassGroupView(
            ClassGroupView classGroup,
            ClassGroupEnrollmentView enrollment,
            List<ContentBlockView> contentBlocks,
            Map<Long, List<BlockContentItemView>> blockContentsByBlock,
            Map<Long, List<LessonView>> blockLessonsByBlock,
            Map<Long, List<AssessmentView>> blockAssessmentsByBlock,
            Map<Long, List<BlockActivityView>> blockActivitiesByBlock,
            boolean eligibleForEnrollment
    ) {
        this.classGroup = classGroup;
        this.enrollment = enrollment;
        this.contentBlocks = List.copyOf(contentBlocks);
        this.blockContentsByBlock = Map.copyOf(blockContentsByBlock);
        this.blockLessonsByBlock = Map.copyOf(blockLessonsByBlock);
        this.blockAssessmentsByBlock = Map.copyOf(blockAssessmentsByBlock);
        this.blockActivitiesByBlock = Map.copyOf(blockActivitiesByBlock);
        this.eligibleForEnrollment = eligibleForEnrollment;
    }

    public static StudentClassGroupView of(
            ClassGroupView classGroup,
            ClassGroupEnrollmentView enrollment,
            List<ContentBlockView> contentBlocks,
            Map<Long, List<BlockContentItemView>> blockContentsByBlock,
            Map<Long, List<LessonView>> blockLessonsByBlock,
            Map<Long, List<AssessmentView>> blockAssessmentsByBlock,
            Map<Long, List<BlockActivityView>> blockActivitiesByBlock,
            boolean eligibleForEnrollment
    ) {
        return new StudentClassGroupView(
                classGroup,
                enrollment,
                contentBlocks,
                blockContentsByBlock,
                blockLessonsByBlock,
                blockAssessmentsByBlock,
                blockActivitiesByBlock,
                eligibleForEnrollment
        );
    }

    public ClassGroupView getClassGroup() {
        return classGroup;
    }

    public ClassGroupEnrollmentView getEnrollment() {
        return enrollment;
    }

    public List<ContentBlockView> getContentBlocks() {
        return contentBlocks;
    }

    public Map<Long, List<BlockContentItemView>> getBlockContentsByBlock() {
        return blockContentsByBlock;
    }

    public Map<Long, List<LessonView>> getBlockLessonsByBlock() {
        return blockLessonsByBlock;
    }

    public Map<Long, List<AssessmentView>> getBlockAssessmentsByBlock() {
        return blockAssessmentsByBlock;
    }

    public Map<Long, List<BlockActivityView>> getBlockActivitiesByBlock() {
        return blockActivitiesByBlock;
    }

    public boolean isEnrolled() {
        return enrollment != null;
    }

    /**
     * A pending request remains visible alongside the active class groups. A
     * finished or withdrawn enrollment does not grant a current place and is
     * therefore represented by the aggregated "without enrollment" card.
     */
    public boolean isCurrentOrPendingEnrollment() {
        return enrollment != null && (enrollment.isActive() || enrollment.isPending());
    }

    public boolean isActiveEnrollment() {
        return enrollment != null && enrollment.isActive();
    }

    public boolean isCanEnroll() {
        return eligibleForEnrollment
                && classGroup.isActive()
                && (enrollment == null || (!enrollment.isActive() && !enrollment.isPending()));
    }

    public boolean isCanWithdraw() {
        return isActiveEnrollment();
    }

    public boolean isActionAvailable() {
        return isActiveEnrollment() || isCanEnroll() || isCanWithdraw();
    }

    public String getUnavailableActionLabel() {
        if (enrollment != null && enrollment.isPending()) {
            return "Waiting approval";
        }
        if (!classGroup.isActive()) {
            return "Closed";
        }
        if (!eligibleForEnrollment) {
            return "Active course enrollment required";
        }
        return "No action available";
    }

    public String getEnrollmentStateLabel() {
        return enrollment == null ? "Not enrolled" : enrollment.getStateLabel();
    }

    public String getEnrollmentBadgeClass() {
        return enrollment == null ? "bg-neutral-30 text-neutral-600" : enrollment.getStateBadgeClass();
    }
}
