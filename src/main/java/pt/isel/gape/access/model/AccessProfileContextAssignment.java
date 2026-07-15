package pt.isel.gape.access.model;

import java.util.Objects;

import pt.isel.gape.security.authorization.AccessEntityType;

public record AccessProfileContextAssignment(
        AccessProfileType profileType,
        AccessEntityType contextType,
        long contextId,
        Long parentContextId
) {

    public AccessProfileContextAssignment {
        profileType = Objects.requireNonNull(profileType, "profileType is required");
        contextType = Objects.requireNonNull(contextType, "contextType is required");
        if (contextId <= 0L) {
            throw new IllegalArgumentException("Profile context id must be positive");
        }
        if (profileType == AccessProfileType.ADMINISTRATOR) {
            throw new IllegalArgumentException("Administrator contexts are handled by administrator permissions");
        }
        switch (profileType) {
            case COORDINATOR -> {
                if (contextType != AccessEntityType.SUBJECT) {
                    throw new IllegalArgumentException("Coordinator context must be a subject");
                }
                parentContextId = null;
            }
            case TEACHER -> {
                if (contextType != AccessEntityType.CLASS_GROUP) {
                    throw new IllegalArgumentException("Teacher context must be a class group");
                }
                parentContextId = null;
            }
            case STUDENT -> {
                if (contextType != AccessEntityType.COURSE) {
                    throw new IllegalArgumentException("Student context must be a course");
                }
                parentContextId = null;
            }
            case ADMINISTRATOR -> throw new IllegalArgumentException("Administrator contexts are handled separately");
        }
    }
}
