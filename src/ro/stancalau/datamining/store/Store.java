package ro.stancalau.datamining.store;

import lombok.ToString;
import ro.stancalau.datamining.model.*;

import java.util.*;
import java.util.stream.Collectors;

@ToString
public class Store {

    private EntityStore<Repository> repositories = new EntityStore<>();
    private EntityStore<Commit> commits = new EntityStore<>();
    private EntityStore<CommitFile> files = new EntityStore<>();
    private EntityStore<Violation> violations = new EntityStore<>();
    private EntityStore<FileChange> fileChanges = new EntityStore<>();
    private EntityStore<FileChangeViolation> fileChangeViolations = new EntityStore<>();
    private EntityStore<CommitViolation> commitViolations = new EntityStore<>();

    public Store() {
    }

    public void addRepository(Repository repository) {
        repositories.put(repository);
    }

    public void addCommit(Commit commit) {
        commits.put(commit);
    }

    public CommitFile getOrCreateCommitFile(String oldPath, String currentPath, Commit commit) {
        String searchName = oldPath == null ? currentPath : oldPath;
        Optional<CommitFile> fileResult = files.getMap().entrySet().stream()
                .filter(v -> v.getValue().getDeletedCommit() == null && v.getValue().getPath().equals(searchName))
                .map(v -> v.getValue())
                .findFirst();
        if (fileResult.isPresent()) {
            return updateCommitFile(oldPath, currentPath, commit, fileResult.get());
        } else {
            CommitFile result = createNewCommitFile(currentPath, commit);
            if (result == null) {
                return files.getMap().entrySet().stream()
                        .filter(v -> v.getValue().getPath().equals(searchName))
                        .map(v -> v.getValue())
                        .collect(Collectors.toCollection(LinkedList::new)).descendingIterator().next();
            } else {
                return result;
            }
        }
    }

    private CommitFile createNewCommitFile(String currentPath, Commit commit) {
        if (currentPath == null) {
            return null;
        }
        CommitFile file = new CommitFile(UUID.randomUUID().toString(), currentPath, commit, null);
        files.put(file);
        return file;
    }

    private CommitFile updateCommitFile(String oldPath, String currentPath, Commit commit, CommitFile fileResult) {
        if (oldPath != null && currentPath != null) {
            fileResult.setPath(currentPath);
        }
        if (currentPath == null) {
            fileResult.setDeletedCommit(commit);
        }
        return fileResult;
    }

    public FileChange addFileChange(CommitFile file, Commit commit, int added, int deleted) {
        FileChange change = new FileChange(UUID.randomUUID().toString(), file, commit, added, deleted);
        fileChanges.put(change);
        return change;
    }

    public Violation getOrCreateViolation(String id, String description) {
        Violation violation = violations.getById(id);
        if (violation == null) {
            violation = new Violation(id, description);
            violations.put(violation);
        }
        return violation;
    }

    public void addFileChangeViolation(FileChange fileChange, Violation violation) {
        fileChangeViolations.put(new FileChangeViolation(UUID.randomUUID().toString(), fileChange, violation));
    }

    public List<Violation> getAllViolations() {
        return violations.getMap().entrySet().stream().map(Map.Entry::getValue)
                .sorted(Comparator.comparing(Violation::getId))
                .collect(Collectors.toList());
    }

    public List<Commit> getCommits() {
        return commits.getMap().entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(Commit::getTimeStamp))
                .collect(Collectors.toList());
    }

    public long countByViolationPerCommit(Commit commit, Violation violation) {
        return commitViolations.getMap().entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(v -> v.getCommit().equals(commit) && v.getViolation().equals(violation))
                .count();
    }

    public long countByViolationPerPrevCommit(Commit commit, Violation violation) {
        return commitViolations.getMap().entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(v -> v.getCommit().getHash().equals(commit.getParentCommitHash()) && v.getViolation().equals(violation))
                .count();
    }

    public List<FileChange> getFileChanges() {
        return fileChanges.getMap().entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(v -> v.getCommit().getTimeStamp()))
                .collect(Collectors.toList());
    }

    public long countByViolationPerPrevChange(FileChange change, Violation violation) {
        Optional<FileChange> prev = getPreviousChange(change);
        if (prev.isPresent()) {
            return countByViolationPerChange(prev.get(), violation);
        } else {
            return 0;
        }
    }

    public Optional<FileChange> getPreviousChange(FileChange change) {
        if (change.getCommit().equals(change.getFile().getAddedCommit())) {
            return Optional.empty();
        }
        return fileChanges.getMap().entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(c -> c.getCommit().getTimeStamp() < change.getCommit().getTimeStamp()
                        && c.getFile().equals(change.getFile())
                        && !c.getId().equals(change.getId()))
                .sorted((a, b) -> (int) (b.getCommit().getTimeStamp() - a.getCommit().getTimeStamp()))
                .findFirst();
    }

    public long countByViolationPerChange(FileChange change, Violation violation) {
        return fileChangeViolations.getMap().entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(v -> v.getFileChange().equals(change) && v.getViolation().equals(violation))
                .count();
    }

    public long countByViolationPerPrevVersionsOfTouchedFIles(Commit commit, Violation violation) {
        return getFileChangesPreviousToCommit(commit).stream()
                .mapToLong(c -> getFileChangeViolationByType(c, violation).size())
                .sum();
    }

    public long countByViolationPerTouched(Commit commit, Violation violation) {
        return getFileChangesInCommit(commit).stream()
                .mapToLong(c -> getFileChangeViolationByType(c, violation).size())
                .sum();
    }

    public Set<FileChange> getFileChangesInCommit(Commit commit) {
        return fileChanges.getMap().values().stream()
                .filter(v -> v.getCommit().equals(commit))
                .collect(Collectors.toSet());
    }

    public Set<FileChange> getFileChangesPreviousToCommit(Commit commit) {
        Set<FileChange> currentChanges = getFileChangesInCommit(commit);
        Set<FileChange> previousChanges = new HashSet<>();
        for (FileChange change : currentChanges) {
            Optional<FileChange> first = fileChanges.getMap().values().stream()
                    .filter(v -> v.getFile().equals(change) && v.getCommit().getTimeStamp() < commit.getTimeStamp())
                    .sorted((a, b) -> (int) (b.getCommit().getTimeStamp() - a.getCommit().getTimeStamp()))
                    .findFirst();
            if (first.isPresent()){
                previousChanges.add(first.get());
            }
        }
        return previousChanges;
    }

    public Set<FileChangeViolation> getFileChangeViolationByType(FileChange fileChange, Violation violation) {
        return fileChangeViolations.getMap().values().stream()
                .filter(v -> v.getFileChange().equals(fileChange) && v.getViolation().equals(violation))
                .collect(Collectors.toSet());
    }

    public CommitViolation createCommitViolation(Commit commitEntity, Violation violationEntity) {
        CommitViolation commitViolation = new CommitViolation(UUID.randomUUID().toString(), commitEntity, violationEntity);
        commitViolations.put(commitViolation);
        return commitViolation;
    }

    public long getTotalViolationCountPerCommit(Commit commit) {
        return commitViolations.getMap().values().stream()
                .filter(v-> v.getCommit().equals(commit))
                .count();
    }
}
