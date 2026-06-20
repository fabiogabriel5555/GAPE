package pt.isel.gape.learning.model;

import java.util.List;

public record ContentRemovalResult(
        ContentDeletionResult deletionResult,
        List<String> orphanedRelativePaths
) {

    public ContentRemovalResult {
        orphanedRelativePaths = orphanedRelativePaths == null
                ? List.of()
                : List.copyOf(orphanedRelativePaths);
    }
}
