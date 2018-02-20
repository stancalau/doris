package ro.stancalau.datamining;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Printer {

    private FileWriter fileWriter;
    private CSVPrinter printer;

    private final List<String> commitTrackingHeader = Arrays.asList("commitId", "author", "fileCount", "addedLineCount", "removedLineCount");


    public Printer(String filePath, List<String> header) throws IOException {
        fileWriter = new FileWriter(filePath);
        printer = CSVFormat.DEFAULT.withHeader(header.toArray(new String[0])).print(fileWriter);
    }

    public void writeEntry(List<String> values) throws IOException {
        printer.printRecord(values);
    }

    public void close() throws IOException {
        printer.flush();
        printer.close();
    }
}
