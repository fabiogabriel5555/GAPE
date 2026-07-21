package pt.isel.gape.transversal.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Context snapshot attached to an audit record.
 *
 * <p>The IDs intentionally do not have foreign keys to their source
 * entities: an audit record must retain its authorization context after the
 * original lesson, class group, content item or user has been removed.</p>
 */
public record ActivityLogScope(
        Set<Long> organizationIds,
        Set<Long> subjectIds,
        Set<Long> classGroupIds,
        Set<Long> userIds
) {

    public ActivityLogScope {
        organizationIds = immutablePositiveIds(organizationIds, "organizationIds");
        subjectIds = immutablePositiveIds(subjectIds, "subjectIds");
        classGroupIds = immutablePositiveIds(classGroupIds, "classGroupIds");
        userIds = immutablePositiveIds(userIds, "userIds");
    }

    public static ActivityLogScope empty() {
        return new ActivityLogScope(Set.of(), Set.of(), Set.of(), Set.of());
    }

    private static Set<Long> immutablePositiveIds(Set<Long> values, String fieldName) {
        Objects.requireNonNull(values, fieldName + " is required");
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException(fieldName + " must only contain positive ids");
            }
            normalized.add(value);
        }
        return Set.copyOf(normalized);
    }
}
