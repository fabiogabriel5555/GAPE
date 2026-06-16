package pt.isel.gape.web.view;

import java.util.List;

public final class StudentClassGroupView {

    private final ClassGroupView classGroup;
    private final ClassGroupEnrollmentView enrollment;
    private final List<ContentBlockView> contentBlocks;
    private final boolean eligibleForEnrollment;

    private StudentClassGroupView(
            ClassGroupView classGroup,
            ClassGroupEnrollmentView enrollment,
            List<ContentBlockView> contentBlocks,
            boolean eligibleForEnrollment
    ) {
        this.classGroup = classGroup;
        this.enrollment = enrollment;
        this.contentBlocks = List.copyOf(contentBlocks);
        this.eligibleForEnrollment = eligibleForEnrollment;
    }

    public static StudentClassGroupView of(
            ClassGroupView classGroup,
            ClassGroupEnrollmentView enrollment,
            List<ContentBlockView> contentBlocks,
            boolean eligibleForEnrollment
    ) {
        return new StudentClassGroupView(classGroup, enrollment, contentBlocks, eligibleForEnrollment);
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
