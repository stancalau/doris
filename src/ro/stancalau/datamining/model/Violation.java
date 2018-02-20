package ro.stancalau.datamining.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class Violation implements Entity {
    @NonNull
    private String id;
    @NonNull
    private String description;
}
