package ro.stancalau.datamining;

import ro.stancalau.datamining.model.FileChange;
import ro.stancalau.datamining.model.Repository;
import ro.stancalau.datamining.model.Violation;
import ro.stancalau.datamining.store.Store;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FileChangeTrackingCSV {

    private final List<String> header = new ArrayList<>(Arrays.asList("fileChangeId", "filePath", "author", "commitId", "addedLines", "deletedLines"));
    private final List<Violation> violations;

    private Printer printer;

    public FileChangeTrackingCSV(Store store, Repository repository) throws IOException {

        violations = Collections.unmodifiableList(store.getAllViolations());
        header.addAll(violations.stream()
                .map(v -> Arrays.asList("prev_" + v.getId(), v.getId())).flatMap(Collection::stream)
                .collect(Collectors.toList()));

        printer = new Printer(repository.getName() + "_fileChangeStats.csv", header);

        for (FileChange change : store.getFileChanges()) {
            List<String> entry = new ArrayList<>();

            entry.add(change.getId());
            entry.add(change.getFile().getPath());
            entry.add(change.getCommit().getAuthor());
            entry.add(change.getCommit().getId());
            entry.add(String.valueOf(change.getAddedLines()));
            entry.add(String.valueOf(change.getDeletedLines()));

            for (Violation v : violations) {
                long prevCount = store.countByViolationPerPrevChange(change, v);
                entry.add(String.valueOf(prevCount));

                long count = store.countByViolationPerChange(change, v);
                entry.add(String.valueOf(count));
            }

            printer.writeEntry(entry);
        }

        printer.close();
    }


}
