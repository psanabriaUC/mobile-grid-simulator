package edu.isistan.useractivity;

import edu.isistan.NodeFileReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UserActivityGenerator {
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
        ACTIVITY_SESSION_DURATION("-as", 2, true, new ArgumentHandler() {
            @Override
            public void handleArguments(String... args) {
                minActivitySessionDuration = parseLongArgument(args[0]);
                maxActivitySessionDuration = parseLongArgument(args[1]);

                if (minActivitySessionDuration > maxActivitySessionDuration) {
                    throw new IllegalArgumentException("maxActivitySessionDuration cannot be lower than minActivitySessionDuration");
                }
            }
        }),
        INACTIVITY_SESSION_DURATION("-is", 2, true, new ArgumentHandler() {
            @Override
            public void handleArguments(String... args) {
                minInactivitySessionDuration = parseLongArgument(args[0]);
                maxInactivitySessionDuration = parseLongArgument(args[1]);

                if (minInactivitySessionDuration > maxInactivitySessionDuration) {
                    throw new IllegalArgumentException("maxInactivitySessionDuration cannot be lower than minInactivitySessionDuration");
                }
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
    private static long minActivitySessionDuration;
    private static long maxActivitySessionDuration;
    private static long minInactivitySessionDuration;
    private static long maxInactivitySessionDuration;

    public static void main(String[] args) {
        parseArguments(args);

        try {
            if (inputFile == null) {
                UserActivityWriter writer = new UserActivityWriter.Builder()
                        .setOutputFile(outputFile)
                        .setMaxTime(maxTime)
                        .setMinActivityIntervalDuration(minActivitySessionDuration)
                        .setMaxActivityIntervalDuration(maxActivitySessionDuration)
                        .setMinInactivityIntervalDuration(minInactivitySessionDuration)
                        .setMaxInactivityIntervalDuration(maxInactivitySessionDuration)
                        .createUserActivityWriter();


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



                    UserActivityWriter writer = new UserActivityWriter.Builder()
                            .setOutputFile(output)
                            .setMaxTime(maxTime)
                            .setMinActivityIntervalDuration(minActivitySessionDuration)
                            .setMaxActivityIntervalDuration(maxActivitySessionDuration)
                            .setMinInactivityIntervalDuration(minInactivitySessionDuration)
                            .setMaxInactivityIntervalDuration(maxInactivitySessionDuration)
                            .createUserActivityWriter();

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
}
