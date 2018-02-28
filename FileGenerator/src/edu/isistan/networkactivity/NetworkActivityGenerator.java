package edu.isistan.networkactivity;

import edu.isistan.NodeFileReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NetworkActivityGenerator {
    private enum FLAGS {
        INPUT_FILE("-i", 1, false, new ArgumentHandler() {
            @Override
            public void handleArguments(String... args) {
                inputFile = args[0];
            }
        }),
        OUTPUT_FILE("-o", 1, true, new ArgumentHandler() {
            @Override
            public void handleArguments(String... args) {
                outputFile = args[0];
            }
        }),
        SIMULATION_DURATION("-t", 1, true, new ArgumentHandler() {
            @Override
            public void handleArguments(String... args) {
                maxTime = parseLongArgument(args[0]);
            }
        }),
        PACKAGE_SIZE("-ps", 2, true, new ArgumentHandler() {
            @Override
            public void handleArguments(String... args) {
                minPackageSize = parseIntArgument(args[0]);
                packageSize1std = parseIntArgument(args[1]);
            }
        }),
        NETWORK_ACTIVITY_INTERVAL("-na", 2, true, new ArgumentHandler() {
            @Override
            public void handleArguments(String... args) {
                minNetworkActivityInterval = parseIntArgument(args[0]);
                networkActivityInterval1std = parseIntArgument(args[1]);
            }
        });

        private String flag;
        private int numberOfArguments;
        private boolean required;
        private ArgumentHandler argumentHandler;
        private boolean argumentHandled;

        FLAGS(String flag, int numberOfArguments, boolean required, ArgumentHandler argumentHandler) {
            this.flag = flag;
            this.numberOfArguments = numberOfArguments;
            this.required = required;
            this.argumentHandler = argumentHandler;
        }

        public void parseArguments(String... args) {
            this.argumentHandler.handleArguments(args);
            argumentHandled = true;
        }
    }

    private interface ArgumentHandler {
        void handleArguments(String... args);
    }

    private static String inputFile;
    private static String outputFile;
    private static long maxTime;
    private static int minPackageSize;
    private static int packageSize1std;
    private static int minNetworkActivityInterval;
    private static int networkActivityInterval1std;


    public static void main(String[] args) {
        parseArguments(args);

        try {
            if (inputFile == null) {
                NetworkActivityWriter writer = new NetworkActivityWriter.Builder()
                        .setOutputFile(outputFile)
                        .setMaxTime(maxTime)
                        .setMinPackageSize(minPackageSize)
                        .setPackageSize1std(packageSize1std)
                        .setMinNetworkActivityInterval(minNetworkActivityInterval)
                        .setNetworkActivityInterval1std(networkActivityInterval1std)
                        .build();

                writer.generateConfigurationFile();

            } else {
                File outputDirectory = new File(outputFile);
                if (outputDirectory.exists()) {
                    File[] files = outputDirectory.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                    outputDirectory.delete();
                }
                outputDirectory.mkdir();

                NodeFileReader reader = new NodeFileReader(inputFile);
                List<String> devices = reader.read();
                for (String device : devices) {
                    String output = outputFile + "/" + device + ".cnf";

                    NetworkActivityWriter writer = new NetworkActivityWriter.Builder()
                            .setOutputFile(output)
                            .setMaxTime(maxTime)
                            .setMinPackageSize(minPackageSize)
                            .setPackageSize1std(packageSize1std)
                            .setMinNetworkActivityInterval(minNetworkActivityInterval)
                            .setNetworkActivityInterval1std(networkActivityInterval1std)
                            .build();

                    writer.generateConfigurationFile();

                    System.out.println(output + ";" + device);

                }
            }
        } catch (IOException e) {
            System.out.println("Error writing output file. Aborting program.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            boolean argumentParsed = false;
            for (FLAGS flag : FLAGS.values()) {
                if (flag.flag.equals(argument)) {
                    if (flag.argumentHandled) {
                        throw new IllegalArgumentException("Unexpected duplicate argument " + flag.flag + ".");
                    }

                    if (args.length <= i + flag.numberOfArguments) {
                        throw new IllegalArgumentException("Insufficient number of arguments for flag " + flag.flag +
                                ". Expected " + flag.numberOfArguments + ", got " + (args.length - i - 1) + ".");
                    }

                    String[] flagArguments = new String[flag.numberOfArguments];
                    for (int j = 0; j < flag.numberOfArguments; j++) {
                        flagArguments[j] = args[i + j + 1];
                    }

                    flag.parseArguments(flagArguments);
                    i += flag.numberOfArguments;
                    argumentParsed = true;

                    break;
                }
            }

            if (!argumentParsed) {
                throw new IllegalArgumentException("Unrecognized flag " + argument + ".");
            }
        }

        for (FLAGS flag : FLAGS.values()) {
            if (flag.required && !flag.argumentHandled) {
                throw new IllegalArgumentException("Missing required argument " + flag.flag + ".");
            }
        }
    }

    private static long parseLongArgument(String arg) {
        long value = 0;
        try {
            value = Long.parseLong(arg);
        } catch (NumberFormatException e) {
            System.out.println("Could not parse argument " + arg + " into long. Aborting program.");
            System.exit(1);
        }

        return value;
    }

    private static int parseIntArgument(String arg) {
        int value = 0;
        try {
            value = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            System.out.println("Could not parse argument " + arg + " into int. Aborting program.");
            System.exit(1);
        }
        return value;
    }
}
