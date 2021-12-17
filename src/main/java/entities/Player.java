package entities;

import lombok.Data;

import java.io.BufferedReader;
import java.io.PrintWriter;

@Data
public class Player {
    private transient BufferedReader reader;
    private transient PrintWriter writer;
    private Role role;
    private String name;
}
