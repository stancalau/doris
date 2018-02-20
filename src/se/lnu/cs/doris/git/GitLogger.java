package se.lnu.cs.doris.git;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.w3c.dom.*;

import org.eclipse.jgit.revwalk.RevCommit;

import org.xml.sax.SAXException;
import ro.stancalau.datamining.model.*;
import ro.stancalau.datamining.store.Store;
import se.lnu.cs.doris.global.GlobalStrings;

/**
 * @author Emil Carlsson
 * <p>
 * This file is a part of Doris
 * <p>
 * Doris is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Doris is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Doris.
 * If not, see <http://www.gnu.org/licenses/>.
 */
public class GitLogger {

    //Used in function parseXMLUnsafeCharacters().
    //Not my prettiest work, but it had to be fixed fast.
    private final static char[] unsafeChars = {0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    public static final String PMD_PROFILE = "pmd.xml";

    public static final String PMD_RESULT_FORMAT = "xml";

    /**
     * Method to add an xml-node to the log for the repository.
     *
     * @param target   Path to the mining base dir.
     * @param repoName Name of the repository.
     * @param id       What number of order the commit is.
     * @param commit   RevCommit of the commit.
     */
    public static void addNode(Store store, ro.stancalau.datamining.model.Repository repoEntity, String target, String repoName, String id, RevCommit commit, RevWalk rw, Repository repo) {
        String path = String.format("%s/%s.xml", target, repoName);
        try {
            //Set up the base path to the xml file.

            //Instantiate some of the tools needed to build the xml document.
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();

            //Used to see if the file exists or not.
            File file = new File(path);

            Document log;
            Element root;

            //If the file doesn't exists a file need to be created
            //and also some root elements etc.
            if (!file.exists()) {
                file.createNewFile();
                log = db.newDocument();
                root = log.createElement(GlobalStrings.PROJECT);
                root.setAttribute(GlobalStrings.PROJECT_NAME, repoName);
                log.appendChild(root);
            } else { //Load the existing document if it exists.
                log = db.parse(file);
                root = (Element) log.getFirstChild();
            }

            //Create a new commit node with attributes.
            Element commitNode = log.createElement(GlobalStrings.COMMIT);
            commitNode.setAttribute(GlobalStrings.COMMIT_NUMBER, id);
            commitNode.setAttribute(GlobalStrings.COMMIT_NAME, commit.getName());
            commitNode.setAttribute(GlobalStrings.COMMIT_TIME, Integer.toString(commit.getCommitTime()));

            //Find all parents. If initial commit no parent node is created.
            if (commit.getParentCount() > 0) {
                for (RevCommit rc : commit.getParents()) {
                    Element parentNode = log.createElement(GlobalStrings.PARENT);
                    parentNode.setAttribute(GlobalStrings.COMMIT_NAME, rc.getName());

                    commitNode.appendChild(parentNode);
                }
            }


            Commit commitEntity = new Commit(id, repoEntity, commit.getAuthorIdent().getName(), commit.getName(),
                    commit.getParentCount() > 0 ? commit.getParent(0).getName() : null, commit.getCommitTime(),
                    0, 0, 0);

            //////////////////STANKY

            String commitSourceCodePath = repoEntity.getName() + "_" + repoEntity.getBranch() + File.separator + commitEntity.getId() + File.separator + "src";
            analyzeVersion(store, commitEntity, commitEntity.getId(), commitSourceCodePath);

            if (commit.getParentCount() > 0) {
                RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
                DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                df.setRepository(repo);
                df.setDiffComparator(RawTextComparator.DEFAULT);
                df.setDetectRenames(true);
                List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
                int filesChanged = diffs.size();
                int linesDeleted = 0;
                int linesAdded = 0;

                Element filesNode = log.createElement(GlobalStrings.FILES);

                for (DiffEntry diff : diffs) {

                    Element fileNode = log.createElement(GlobalStrings.FILE);
                    String newPath = null;
                    if (diff.getChangeType() != DiffEntry.ChangeType.DELETE) {
                        newPath = diff.getNewPath();
                        fileNode.setAttribute(GlobalStrings.NEW_PATH, newPath);
                    }
                    String oldPath = null;
                    if (diff.getChangeType() == DiffEntry.ChangeType.RENAME || diff.getChangeType() == DiffEntry.ChangeType.DELETE) {
                        oldPath = diff.getOldPath();
                        fileNode.setAttribute(GlobalStrings.OLD_PATH, oldPath);
                    }
                    System.out.println("__ " + diff.getChangeType() + "  " + newPath + " " + oldPath + " " + commitEntity.getHash());

                    CommitFile commitFile;
                    try {
                        commitFile = store.getOrCreateCommitFile(oldPath, newPath, commitEntity);
                    } catch (NoSuchElementException nee) {
                        continue;
                    }

                    filesNode.appendChild(fileNode);
                    int fileLinesDeleted = 0;
                    int fileLinesAdded = 0;

                    for (Edit edit : df.toFileHeader(diff).toEditList()) {
                        fileLinesDeleted = edit.getEndA() - edit.getBeginA();
                        linesDeleted += fileLinesDeleted;
                        fileLinesAdded = edit.getEndB() - edit.getBeginB();
                        linesAdded += fileLinesAdded;
                    }

                    FileChange fileChange = store.addFileChange(commitFile, commitEntity, fileLinesAdded, fileLinesDeleted);

                    if (newPath != null && newPath.endsWith(".java")) {

                        String xmlPath = "x_" + new Random().nextInt(Integer.MAX_VALUE) + ".xml";
                        String javaFile = String.format("%s%s%s%s%s", target, File.separator, id, File.separator, newPath);

                        PMDConfiguration config = new PMDConfiguration();
                        config.setReportFormat(PMD_RESULT_FORMAT);
                        config.setInputPaths(javaFile);
                        config.setRuleSets(PMD_PROFILE);
                        config.setReportFile(xmlPath);
                        PMD.doPMD(config);

                        Element violationsNode = log.createElement(GlobalStrings.VIOLATIONS);
                        File fXmlFile = new File(xmlPath);
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(fXmlFile);
                        doc.getDocumentElement().normalize();

                        NodeList fileNodeList = doc.getElementsByTagName("file");
                        if (fileNodeList.getLength() > 0) {
                            NodeList violationNodeList = ((Element) fileNodeList.item(0)).getElementsByTagName("violation");

                            for (int i = 0; i < violationNodeList.getLength(); i++) {
                                Element violation = (Element) log.importNode(violationNodeList.item(i), true);
                                violationsNode.appendChild(violation.cloneNode(true));

                                Violation violationEntity = store.getOrCreateViolation(violation.getAttribute("rule"), violation.getTextContent());
                                store.addFileChangeViolation(fileChange, violationEntity);
                            }
                        }
                        fileNode.appendChild(violationsNode);

                        fXmlFile.delete();
                    }
                }

                Element statNode = log.createElement(GlobalStrings.STATS);
                statNode.setAttribute(GlobalStrings.FILE_COUNT, String.valueOf(filesChanged));
                statNode.setAttribute(GlobalStrings.ADDED_LINES, String.valueOf(linesAdded));
                statNode.setAttribute(GlobalStrings.REMOVED_LINES, String.valueOf(linesDeleted));
                statNode.appendChild(filesNode);
                commitNode.appendChild(statNode);

                commitEntity.setFileCount(filesChanged);
                commitEntity.setAddedLines(linesAdded);
                commitEntity.setDeletedLines(linesDeleted);

            }
            //////////////////////STANKY^^


            //Populate with other nodes.
            Element authorNode = log.createElement(GlobalStrings.AUTHOR);
            authorNode.setAttribute(GlobalStrings.NAME, commit.getAuthorIdent().getName());
            authorNode.setAttribute(GlobalStrings.E_MAIL, commit.getAuthorIdent().getEmailAddress());


            commitNode.appendChild(authorNode);

            Element committerNode = log.createElement(GlobalStrings.COMMITTER);
            committerNode.setAttribute(GlobalStrings.NAME, commit.getCommitterIdent().getName());
            committerNode.setAttribute(GlobalStrings.E_MAIL, commit.getCommitterIdent().getEmailAddress());

            commitNode.appendChild(committerNode);

            Element messageNode = log.createElement(GlobalStrings.COMMIT_MESSAGE);
            //Ugly quick fix to repair broken XML returns.
            byte[] messageByteArray = commit.getFullMessage().getBytes("UTF-8");
            String message = parseXMLUnsafeCharacters(new String(messageByteArray, GlobalStrings.UTF8_CHATSET));
            messageNode.appendChild(log.createTextNode(message));

            commitNode.appendChild(messageNode);

            root.appendChild(commitNode);

            //Write to file.
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            DOMSource source = new DOMSource(log);
            StreamResult result = new StreamResult(path);

            t.transform(source, result);

            store.addCommit(commitEntity);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Set<CommitViolation> analyzeVersion(Store store, Commit commitEntity, String id, String path) throws ParserConfigurationException, IOException, SAXException {

        System.out.println(path);

        String xmlPath = "commit_" + id + ".xml";

        PMDConfiguration config = new PMDConfiguration();
        config.setReportFormat(PMD_RESULT_FORMAT);
        config.setInputPaths(path);
        config.setRuleSets(PMD_PROFILE);
        config.setReportFile(xmlPath);
        PMD.doPMD(config);

        File fXmlFile = new File(xmlPath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();


        NodeList fileNodeList = doc.getElementsByTagName("file");
        for (int i = 0; i < fileNodeList.getLength(); i++) {
            NodeList violationNodeList = ((Element) fileNodeList.item(i)).getElementsByTagName("violation");

            for (int j = 0; j < violationNodeList.getLength(); j++) {
                Element violation = (Element) violationNodeList.item(j);
                Violation violationEntity = store.getOrCreateViolation(violation.getAttribute("rule"), violation.getTextContent());
                store.createCommitViolation(commitEntity, violationEntity);
            }
        }

        fXmlFile.delete();
        return Collections.emptySet();
    }

    private static void execute(String command) throws IOException, InterruptedException {

        Process process = Runtime.getRuntime().exec(command);

        InputStream inputStream = process.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
        }

        process.waitFor();
    }


    /**
     * Removes characters that isn't safe for XML and replaces them
     * with a single space.
     *
     * @param s String to be parsed
     * @return Parsed String.
     */
    private static String parseXMLUnsafeCharacters(String s) {
        for (char c : unsafeChars) {
            s = s.replace(c, (char) 32);
        }

        return s;
    }

}
