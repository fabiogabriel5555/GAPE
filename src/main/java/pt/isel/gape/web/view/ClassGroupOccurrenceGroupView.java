package pt.isel.gape.web.view;

import java.util.List;

public final class ClassGroupOccurrenceGroupView {

    private final long occurrenceId;
    private final long occurrencePeriodId;
    private final String occurrenceLabel;
    private final String occurrenceDateRangeLabel;
    private final String occurrenceStateLabel;
    private final String occurrenceStateBadgeClass;
    private final List<ClassGroupView> classGroups;

    public ClassGroupOccurrenceGroupView(
            long occurrenceId,
            String occurrenceLabel,
            String occurrenceDateRangeLabel,
            String occurrenceStateLabel,
            String occurrenceStateBadgeClass,
            List<ClassGroupView> classGroups
    ) {
        this(
                occurrenceId,
                0,
                occurrenceLabel,
                occurrenceDateRangeLabel,
                occurrenceStateLabel,
                occurrenceStateBadgeClass,
                classGroups
        );
    }

    public ClassGroupOccurrenceGroupView(
            long occurrenceId,
            long occurrencePeriodId,
            String occurrenceLabel,
            String occurrenceDateRangeLabel,
            String occurrenceStateLabel,
            String occurrenceStateBadgeClass,
            List<ClassGroupView> classGroups
    ) {
        this.occurrenceId = occurrenceId;
        this.occurrencePeriodId = occurrencePeriodId;
        this.occurrenceLabel = occurrenceLabel;
        this.occurrenceDateRangeLabel = occurrenceDateRangeLabel;
        this.occurrenceStateLabel = occurrenceStateLabel;
        this.occurrenceStateBadgeClass = occurrenceStateBadgeClass;
        this.classGroups = classGroups == null ? List.of() : List.copyOf(classGroups);
    }

    public long getOccurrenceId() {
        return occurrenceId;
    }

    public long getOccurrencePeriodId() {
        return occurrencePeriodId;
    }

    /** Uses the concrete occurrence period as the stable disclosure key. */
    public long getDisclosureId() {
        return occurrencePeriodId > 0 ? occurrencePeriodId : occurrenceId;
    }

    public String getOccurrenceLabel() {
        return occurrenceLabel;
    }

    public String getOccurrenceDateRangeLabel() {
        return occurrenceDateRangeLabel;
    }

    public String getOccurrenceStateLabel() {
        return occurrenceStateLabel;
    }

    public String getOccurrenceStateBadgeClass() {
        return occurrenceStateBadgeClass;
    }

    public List<ClassGroupView> getClassGroups() {
        return classGroups;
    }

    public int getClassGroupCount() {
        return classGroups.size();
    }
}
