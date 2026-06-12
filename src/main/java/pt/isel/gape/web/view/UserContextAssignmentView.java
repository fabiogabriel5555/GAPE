package pt.isel.gape.web.view;

public final class UserContextAssignmentView {

    private final String group;
    private final String label;
    private final String detail;
    private final String detailHtml;

    public UserContextAssignmentView(String group, String label, String detail) {
        this(group, label, detail, null);
    }

    public UserContextAssignmentView(String group, String label, String detail, String detailHtml) {
        this.group = group == null ? "" : group;
        this.label = label == null ? "" : label;
        this.detail = detail == null ? "" : detail;
        this.detailHtml = detailHtml == null ? "" : detailHtml;
    }

    public String getGroup() {
        return group;
    }

    public String getLabel() {
        return label;
    }

    public String getDetail() {
        return detail;
    }

    public String getDetailHtml() {
        return detailHtml;
    }

    public boolean isHasDetailHtml() {
        return !detailHtml.isBlank();
    }
}
