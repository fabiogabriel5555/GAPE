package pt.isel.gape.web.view;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.security.authorization.AccessEntityType;

public final class ProfileContextOptionView {

    private final AccessProfileType profileType;
    private final AccessEntityType contextType;
    private final long contextId;
    private final Long parentContextId;
    private final String label;
    private final String detail;
    private final String nodeKey;
    private final String parentKey;
    private final int hierarchyDepth;
    private final boolean selectable;

    public ProfileContextOptionView(
            AccessProfileType profileType,
            AccessEntityType contextType,
            long contextId,
            Long parentContextId,
            String label,
            String detail,
            String nodeKey,
            String parentKey,
            int hierarchyDepth,
            boolean selectable
    ) {
        this.profileType = profileType;
        this.contextType = contextType;
        this.contextId = contextId;
        this.parentContextId = parentContextId;
        this.label = label;
        this.detail = detail;
        this.nodeKey = nodeKey == null || nodeKey.isBlank() ? contextType + ":" + contextId : nodeKey;
        this.parentKey = parentKey == null ? "" : parentKey;
        this.hierarchyDepth = Math.max(0, hierarchyDepth);
        this.selectable = selectable;
    }

    public String getProfileType() {
        return profileType.name();
    }

    public String getContextType() {
        return contextType.name();
    }

    public long getContextId() {
        return contextId;
    }

    public long getParentContextIdValue() {
        return parentContextId == null ? 0L : parentContextId;
    }

    public String getLabel() {
        return label;
    }

    public String getDetail() {
        return detail;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public String getParentKey() {
        return parentKey;
    }

    public int getHierarchyIndent() {
        return hierarchyDepth * 24;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public String getElementId() {
        return "profileContext" + (profileType.name() + ":" + nodeKey).replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public String getValue() {
        return profileType.name() + ":" + contextType.name() + ":" + contextId + ":" + getParentContextIdValue();
    }
}
