package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class CommunicationUtils {
    public static void send(String information, PrintWriter... writers) {
        Arrays.stream(writers).forEach(writer -> writer.println(information));
    }

    public static String receive(BufferedReader reader) throws IOException {
        return reader.readLine();
    }
}
