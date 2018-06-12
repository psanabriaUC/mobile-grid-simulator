package edu.isistan.useractivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class UserActivityWriter {
    private enum ScreenStatus {
        ON("ON"),
        OFF("OFF");

        private String value;

        ScreenStatus(String value) {
            this.value = value;
        }
    }

    private String outputFile;
    private long maxTime;
    private long minActivityIntervalDuration;
    private long maxActivityIntervalDuration;
    private long minInactivityIntervalDuration;
    private long maxInactivityIntervalDuration;

    private static Random random = new Random();

    private UserActivityWriter(String outputFile,
                              long maxTime,
                              long minActivityIntervalDuration,
                              long maxActivityIntervalDuration,
                              long minInactivityIntervalDuration,
                              long maxInactivityIntervalDuration) {
        this.outputFile = outputFile;
        this.maxTime = maxTime;
        this.minActivityIntervalDuration = minActivityIntervalDuration;
        this.maxActivityIntervalDuration = maxActivityIntervalDuration;
        this.minInactivityIntervalDuration = minInactivityIntervalDuration;
        this.maxInactivityIntervalDuration = maxInactivityIntervalDuration;

        if (this.minActivityIntervalDuration > this.maxActivityIntervalDuration) {
            throw new IllegalArgumentException("maxActivityIntervalDuration cannot be lower than minActivityIntervalDuration");
        }

        if (this.minInactivityIntervalDuration > this.maxInactivityIntervalDuration) {
            throw new IllegalArgumentException("maxInactivityIntervalDuration cannot be lower than minInactivityIntervalDuration");
        }
    }

    public void generateConfigurationFile() throws IOException {
        File file = new File(this.outputFile);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);

            long time = 0;

            while(time < this.maxTime) {
                long screenOnTime = time + (long) ((this.maxInactivityIntervalDuration - this.minInactivityIntervalDuration) * random.nextDouble());
                long screenOffTime = screenOnTime + (long) ((this.maxActivityIntervalDuration - this.minActivityIntervalDuration) * random.nextDouble());

                time = screenOffTime;

                if (screenOffTime > maxTime) break;

                writeLine(fileWriter, screenOnTime, ScreenStatus.ON);
                writeLine(fileWriter, screenOffTime, ScreenStatus.OFF);
            }
        } catch (IOException e) {
            file.delete();
            throw e;
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }


    }

    private void writeLine(FileWriter writer, long timestamp, ScreenStatus screenStatus) throws IOException {
        writer.write("" + timestamp + ";" + screenStatus.value + "\n");
    }

    public static class Builder {
        private String outputFile;
        private long maxTime;
        private long minActivityIntervalDuration;
        private long maxActivityIntervalDuration;
        private long minInactivityIntervalDuration;
        private long maxInactivityIntervalDuration;

        public Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public Builder setMaxTime(long maxTime) {
            this.maxTime = maxTime;
            return this;
        }

        public Builder setMinActivityIntervalDuration(long minActivityIntervalDuration) {
            this.minActivityIntervalDuration = minActivityIntervalDuration;
            return this;
        }

        public Builder setMaxActivityIntervalDuration(long maxActivityIntervalDuration) {
            this.maxActivityIntervalDuration = maxActivityIntervalDuration;
            return this;
        }

        public Builder setMinInactivityIntervalDuration(long minInactivityIntervalDuration) {
            this.minInactivityIntervalDuration = minInactivityIntervalDuration;
            return this;
        }

        public Builder setMaxInactivityIntervalDuration(long maxInactivityIntervalDuration) {
            this.maxInactivityIntervalDuration = maxInactivityIntervalDuration;
            return this;
        }

        public UserActivityWriter createUserActivityWriter() {
            return new UserActivityWriter(outputFile, maxTime, minActivityIntervalDuration, maxActivityIntervalDuration, minInactivityIntervalDuration, maxInactivityIntervalDuration);
        }
    }
}
