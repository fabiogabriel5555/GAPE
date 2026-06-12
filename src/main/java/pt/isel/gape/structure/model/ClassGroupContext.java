package pt.isel.gape.structure.model;

public record ClassGroupContext(
        long id,
        long courseId,
        long subjectId,
        long organizationId,
        Long organicUnitId,
        String code,
        String organizationName,
        String organizationAcronym,
        String organicUnitName,
        String organicUnitAcronym,
        String courseName,
        String courseAcronym,
        String subjectName,
        String subjectAcronym,
        String state
) {

    public boolean isActive() {
        return "active".equalsIgnoreCase(state);
    }

    public String label() {
        return code == null || code.isBlank() ? "Class group " + id : code;
    }

    public String detail() {
        return courseName + " / " + subjectName;
    }
}
