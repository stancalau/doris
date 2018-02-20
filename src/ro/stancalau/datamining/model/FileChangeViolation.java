package ro.stancalau.datamining.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class FileChangeViolation implements Entity {
    @NonNull
    private String id;
    @NonNull
    private FileChange fileChange;
    @NonNull
    private Violation violation;
}
