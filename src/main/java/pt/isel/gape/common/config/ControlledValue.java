package pt.isel.gape.common.config;

import java.util.Objects;

public final class ControlledValue {

    private final String code;
    private final String label;

    public ControlledValue(String code, String label) {
        this.code = Objects.requireNonNull(code, "code is required");
        this.label = Objects.requireNonNull(label, "label is required");
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
