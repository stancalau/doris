package ro.stancalau.datamining.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class CommitViolation implements Entity {
    @NonNull
    private String id;
    @NonNull
    private Commit commit;
    @NonNull
    private Violation violation;
}
