package pt.isel.gape.web.view;

public final class SelectOptionView {

    private final String value;
    private final String label;
    private final String title;
    private final boolean selected;

    public SelectOptionView(String value, String label, boolean selected) {
        this(value, label, label, selected);
    }

    public SelectOptionView(String value, String label, String title, boolean selected) {
        this.value = value;
        this.label = label;
        this.title = title;
        this.selected = selected;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getTitle() {
        return title;
    }

    public boolean isSelected() {
        return selected;
    }
}
