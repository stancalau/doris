package ro.stancalau.datamining.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CommitFile implements Entity {
    @NonNull
    private String id;
    @NonNull
    private String path;
    @NonNull
    private Commit addedCommit;

    private Commit deletedCommit;
}
