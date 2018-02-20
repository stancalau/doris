package ro.stancalau.datamining.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class Repository implements Entity {

    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private String path;
    @NonNull String branch;

}
