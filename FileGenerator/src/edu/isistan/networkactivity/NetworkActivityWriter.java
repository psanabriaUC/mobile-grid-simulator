package edu.isistan.networkactivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class NetworkActivityWriter {
    private enum MessageDirection {
        INCOMING("IN"), OUTGOING("OUT");

        private String value;

        MessageDirection(String value) {
            this.value = value;
        }
    }

    private static Random random = new Random();

    private String outputFile;
    private long maxTime;
    private int minPackageSize;
    private int packageSize1std;
    private int minNetworkActivityInterval;
    private int networkActivityInterval1std;

    NetworkActivityWriter(String outputFile, long maxTime, int minPackageSize, int packageSize1std, int minNetworkActivityInterval, int networkActivityInterval1std) {
        this.outputFile = outputFile;
        this.maxTime = maxTime;
        this.minPackageSize = minPackageSize;
        this.packageSize1std = packageSize1std;
        this.minNetworkActivityInterval = minNetworkActivityInterval;
        this.networkActivityInterval1std = networkActivityInterval1std;
    }

    public void generateConfigurationFile() throws IOException {
        File file = new File(this.outputFile);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);

            long time = 0;

            while(time < this.maxTime) {
                long timestamp = time + minNetworkActivityInterval + (long) (networkActivityInterval1std * Math.abs(random.nextGaussian()));
                int packageSize = minPackageSize + (int) (packageSize1std * Math.abs(random.nextGaussian()));
                MessageDirection direction = random.nextInt(2) == 0 ? MessageDirection.INCOMING : MessageDirection.OUTGOING;

                time = timestamp;

                if (time > maxTime) break;

                writeLine(fileWriter, timestamp, packageSize, direction);
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

    private void writeLine(FileWriter writer, long timestamp, int packageSize, MessageDirection direction) throws IOException {
        writer.write("" + timestamp + ";" + packageSize + ";" + direction.value + "\n");
    }

    public static class Builder {
        private String outputFile;
        private long maxTime;
        private int minPackageSize;
        private int packageSize1std;
        private int minNetworkActivityInterval;
        private int networkActivityInterval1std;

        public Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public Builder setMaxTime(long maxTime) {
            this.maxTime = maxTime;
            return this;
        }

        public Builder setMinPackageSize(int minPackageSize) {
            this.minPackageSize = minPackageSize;
            return this;
        }

        public Builder setPackageSize1std(int packageSize1std) {
            this.packageSize1std = packageSize1std;
            return this;
        }

        public Builder setMinNetworkActivityInterval(int minNetworkActivityInterval) {
            this.minNetworkActivityInterval = minNetworkActivityInterval;
            return this;
        }

        public Builder setNetworkActivityInterval1std(int networkActivityInterval1std) {
            this.networkActivityInterval1std = networkActivityInterval1std;
            return this;
        }

        public NetworkActivityWriter build() {
            return new NetworkActivityWriter(outputFile, maxTime, minPackageSize, packageSize1std, minNetworkActivityInterval, networkActivityInterval1std);
        }
    }
}
