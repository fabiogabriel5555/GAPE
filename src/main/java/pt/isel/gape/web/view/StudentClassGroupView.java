package pt.isel.gape.web.view;

import java.util.List;
import java.util.Map;

public final class StudentClassGroupView {

    private final ClassGroupView classGroup;
    private final ClassGroupEnrollmentView enrollment;
    private final List<ContentBlockView> contentBlocks;
    private final Map<Long, List<BlockContentItemView>> blockContentsByBlock;
    private final boolean eligibleForEnrollment;

    private StudentClassGroupView(
            ClassGroupView classGroup,
            ClassGroupEnrollmentView enrollment,
            List<ContentBlockView> contentBlocks,
            Map<Long, List<BlockContentItemView>> blockContentsByBlock,
            boolean eligibleForEnrollment
    ) {
        this.classGroup = classGroup;
        this.enrollment = enrollment;
        this.contentBlocks = List.copyOf(contentBlocks);
        this.blockContentsByBlock = Map.copyOf(blockContentsByBlock);
        this.eligibleForEnrollment = eligibleForEnrollment;
    }

    public static StudentClassGroupView of(
            ClassGroupView classGroup,
            ClassGroupEnrollmentView enrollment,
            List<ContentBlockView> contentBlocks,
            Map<Long, List<BlockContentItemView>> blockContentsByBlock,
            boolean eligibleForEnrollment
    ) {
        return new StudentClassGroupView(
                classGroup,
                enrollment,
                contentBlocks,
                blockContentsByBlock,
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

    public boolean isEnrolled() {
        return enrollment != null;
    }

    public boolean isActiveEnrollment() {
        return enrollment != null && enrollment.isActive();
    }

    public boolean isCanEnroll() {
        return eligibleForEnrollment && enrollment == null && classGroup.isActive();
    }

    public boolean isCanWithdraw() {
        return isActiveEnrollment();
    }

    public String getEnrollmentStateLabel() {
        return enrollment == null ? "Not enrolled" : enrollment.getStateLabel();
    }

    public String getEnrollmentBadgeClass() {
        return enrollment == null ? "bg-neutral-30 text-neutral-600" : enrollment.getStateBadgeClass();
    }
}
