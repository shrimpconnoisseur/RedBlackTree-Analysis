package rbtree;

import java.io.IOException;
import java.util.*;

/*
* Loads the NASA Planetary Systems CSV
* Builds the Red-Black Tree
* Runs analysis operations on RBT
* Writes to JSON for Python visualisation.
*
* In a terminal, compile first:
*   javac -d out src/rbtree/*.java
*
* Then, run:
*   java -cp out rbtree.Main <absolute-path-to-csv> [output.json]
*
* If no JSON path is provided, the file is written to "rbt_export.json"
*   in the current working directory.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -cp out rbtree.Main <planets.csv> [output.json]");
            return;
        }

        String csvPath = args[0];
        String jsonPath = args.length >= 2 ? args[1] : "rbt_export.json";

        // 1. LOAD CSV
        List<Planet> planets;
        try {
            planets = CSVLoader.load(csvPath);
        }
        catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
            return;
        }

        if (planets.isEmpty()) {
            System.err.println("No valid records found.");
            return;
        }

        System.out.printf("CSV Loaded: %,d planets found.", planets.size());

        // 2. BUILD RED-BLACK TREE
        RedBlackTree<Planet> tree = new RedBlackTree<>();
        long t0 = System.nanoTime();
        for (Planet p : planets) tree.insert(p);
        long buildMs = (System.nanoTime() - t0) / 1_000_000;

        String violation = tree.verifyRules();
        if (!violation.isEmpty()) {
            System.err.println("Violations found: " + violation);
            return;
        }
        System.out.printf("Red-Black Tree built successfully. Build time: %,d ms\n", buildMs);

        // 3. OUTPUT TREE STATS (DEBUG)
        double theoreticalMax = 2.0 * Math.log(tree.size() + 1) / Math.log(2);
        System.out.printf(
                "Tree Stats:%n" +
                " Nodes : %,d%n" +
                " Height : %d (theoretical max ~%.1f)%n" +
                " Black-height: %d%n" +
                " Rotations: %,d%n" +
                " Recolourings: %,d%n",
                tree.size(),
                tree.height(), theoreticalMax,
                tree.blackHeight(),
                tree.getTotalRotations(),
                tree.getTotalRecolourings()
        );

        // 4. EXPORT TO JSON
        try {
            JSONExporter.export(tree, jsonPath);
            System.out.printf("Tree export to: %s%n", jsonPath);
        } catch (IOException e) {
            System.err.println("Error writing to JSON: " + e.getMessage());
        }
    }
}
