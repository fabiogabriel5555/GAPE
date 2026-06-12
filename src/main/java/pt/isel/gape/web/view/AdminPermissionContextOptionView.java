package pt.isel.gape.web.view;

public final class AdminPermissionContextOptionView {

    private final String permissionCode;
    private final String contextType;
    private final long contextId;
    private final String label;
    private final String detail;
    private final String nodeKey;
    private final String parentKey;
    private final int hierarchyDepth;
    private final boolean selectable;

    public AdminPermissionContextOptionView(
            String permissionCode,
            String contextType,
            long contextId,
            String label,
            String detail
    ) {
        this(permissionCode, contextType, contextId, label, detail, contextType + ":" + contextId, "", 0, true);
    }

    public AdminPermissionContextOptionView(
            String permissionCode,
            String contextType,
            long contextId,
            String label,
            String detail,
            String parentKey,
            int hierarchyDepth
    ) {
        this(permissionCode, contextType, contextId, label, detail, contextType + ":" + contextId, parentKey, hierarchyDepth, true);
    }

    public AdminPermissionContextOptionView(
            String permissionCode,
            String contextType,
            long contextId,
            String label,
            String detail,
            String nodeKey,
            String parentKey,
            int hierarchyDepth
    ) {
        this(permissionCode, contextType, contextId, label, detail, nodeKey, parentKey, hierarchyDepth, true);
    }

    public AdminPermissionContextOptionView(
            String permissionCode,
            String contextType,
            long contextId,
            String label,
            String detail,
            String nodeKey,
            String parentKey,
            int hierarchyDepth,
            boolean selectable
    ) {
        this.permissionCode = permissionCode;
        this.contextType = contextType;
        this.contextId = contextId;
        this.label = label;
        this.detail = detail;
        this.nodeKey = nodeKey == null || nodeKey.isBlank() ? contextType + ":" + contextId : nodeKey;
        this.parentKey = parentKey == null ? "" : parentKey;
        this.hierarchyDepth = Math.max(0, hierarchyDepth);
        this.selectable = selectable;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public String getContextType() {
        return contextType;
    }

    public long getContextId() {
        return contextId;
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

    public String getElementId() {
        return "adminPermission" + (permissionCode + ":" + nodeKey).replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public String getParentKey() {
        return parentKey;
    }

    public int getHierarchyDepth() {
        return hierarchyDepth;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public int getHierarchyIndent() {
        return hierarchyDepth * 24;
    }

    public String getValue() {
        return permissionCode + ":" + contextType + ":" + contextId;
    }
}
