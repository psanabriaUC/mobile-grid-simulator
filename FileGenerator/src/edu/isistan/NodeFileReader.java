package edu.isistan;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NodeFileReader {
    private File file;

    public NodeFileReader(String filePath) {
        this.file = new File(filePath);
    }

    public List<String> read() throws FileNotFoundException {
        List<String> deviceNames = new ArrayList<>();

        Scanner scanner = new Scanner(this.file);
        while(scanner.hasNext()) {
            String line = scanner.nextLine().trim();

            if (line.indexOf("#") == 0 || line.length() == 0) continue;

            String[] values = line.split(";");
            deviceNames.add(values[0]);
        }
        scanner.close();

        return deviceNames;
    }


}
