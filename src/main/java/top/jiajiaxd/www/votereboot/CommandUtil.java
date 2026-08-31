package top.jiajiaxd.www.votereboot;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public final class CommandUtil {

    private CommandUtil() {
    }

    public static String run(String command) throws IOException {
        return execute(Runtime.getRuntime().exec(command), command);
    }

    public static String run(String[] command) throws IOException {
        return execute(Runtime.getRuntime().exec(command), String.join(" ", command));
    }

    private static String execute(Process process, String command) {
        StringBuilder result = new StringBuilder();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            try (Scanner input = new Scanner(process.getInputStream())) {
                while (input.hasNextLine()) {
                    result.append(input.nextLine()).append('\n');
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        return command + "\n" + result;
    }
}
