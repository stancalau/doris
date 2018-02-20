package ro.stancalau.datamining.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Commit implements Entity {

    @NonNull
    private String id;
    @NonNull
    private Repository repository;
    @NonNull
    private String author;
    @NonNull
    private String hash;

    private String parentCommitHash;
    @NonNull
    private long timeStamp;
    @NonNull
    private int fileCount;
    @NonNull
    private int addedLines;
    @NonNull
    private int deletedLines;

}
