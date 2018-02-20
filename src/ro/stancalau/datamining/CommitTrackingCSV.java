package ro.stancalau.datamining;

import ro.stancalau.datamining.model.Commit;
import ro.stancalau.datamining.model.Repository;
import ro.stancalau.datamining.model.Violation;
import ro.stancalau.datamining.store.Store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommitTrackingCSV {

    private final List<String> header = new ArrayList<>(Arrays.asList("commitId", "author", "fileCount", "addedLines", "deletedLines", "totalViolationCount"));
    private final List<Violation> violations;

    private Printer printer;

    public CommitTrackingCSV(Store store, Repository repository) throws IOException {

        violations = Collections.unmodifiableList(store.getAllViolations());
        header.addAll(violations.stream()
                .map(v -> Arrays.asList("prev_total" + v.getId(), "current_total" + v.getId(), "prevFileVersions_" + v.getId(), "currentFileVersions_" + v.getId()))
                .flatMap(e -> e.stream())
                .collect(Collectors.toList()));

        printer = new Printer(repository.getName() + "_commitStats.csv", header);

        for (Commit commit : store.getCommits()) {
            List<String> entry = new ArrayList<>();

            entry.add(commit.getId());
            entry.add(commit.getAuthor());
            entry.add(String.valueOf(commit.getFileCount()));
            entry.add(String.valueOf(commit.getAddedLines()));
            entry.add(String.valueOf(commit.getDeletedLines()));
            entry.add(String.valueOf(store.getTotalViolationCountPerCommit(commit)));

            for (Violation v : violations) {
                long prevTotalCount = store.countByViolationPerPrevCommit(commit, v);
                entry.add(String.valueOf(prevTotalCount));

                long currentTotalCount = store.countByViolationPerCommit(commit, v);
                entry.add(String.valueOf(currentTotalCount));

                long prevFileVersionsCount = store.countByViolationPerPrevVersionsOfTouchedFIles(commit, v);
                entry.add(String.valueOf(prevFileVersionsCount));

                long currentFileVersionsCount = store.countByViolationPerTouched(commit, v);
                entry.add(String.valueOf(currentFileVersionsCount));

            }

            printer.writeEntry(entry);
        }

        printer.close();
    }


}
