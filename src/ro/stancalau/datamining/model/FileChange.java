package ro.stancalau.datamining.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class FileChange implements Entity {
    @NonNull
    private String id;
    @NonNull
    private CommitFile file;
    @NonNull
    private Commit commit;
    @NonNull
    private int addedLines;
    @NonNull
    private int deletedLines;
}
