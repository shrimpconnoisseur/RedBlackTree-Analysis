package rbtree;

import java.io.*;
import java.util.*;

public class JSONExporter {
    public static void export(RedBlackTree<Planet> tree, String filePath) throws IOException {
        List<String> nodeJsons = new ArrayList<String>();

        tree.exportNodes((id, planet, isRed, leftId, rightId) -> {
            String entry = String.format(
                    " {%n" +
                    " \"id\": %d,%n" +
                    " \"color\": \"%s\",%n" +
                    " \"orbital_period\": %s,%n" +
                    " \"planet_name\": %s,%n" +
                    " \"host_name\": %s,%n" +
                    " \"discovery_year\": %d,%n" +
                    " \"discovery_method\": %s,%n" +
                    " \"radius_earth\": %s,%n" +
                    " \"mass_earth\": %s,%n" +
                    " \"distance_pc\": %s,%n" +
                    " \"left_id\": %d,%n" +
                    " \"right_id\": %d%n" +
                    " }",
                    id,
                    isRed ? "RED" : "BLACK",
                    planet.orbitalPeriod,
                    jsonString(planet.planetName),
                    jsonString(planet.hostName),
                    planet.discoveryYear,
                    jsonString(planet.discoveryMethod),
                    jsonDouble(planet.radiusEarth),
                    jsonDouble(planet.massEarth),
                    jsonDouble(planet.distance),
                    leftId,
                    rightId
            );
            nodeJsons.add(entry);
        });

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath)))) {
            pw.println("{");
            pw.println(" \"metadata\": {");
            pw.printf(" \"size\": %d,%n", tree.size());
            pw.printf(" \"height\": %d,%n", tree.height());
            pw.printf(" \"black_height\": %d,%n", tree.blackHeight());
            pw.printf(" \"rotations\": %d,%n", tree.getTotalRotations());
            pw.printf(" \"recolourings\": %d%n", tree.getTotalRecolourings());
            pw.println(" },");

            pw.println(" \"nodes\": [");
            for (int i = 0; i < nodeJsons.size(); i++) {
                pw.println(nodeJsons.get(i));
                if (i < nodeJsons.size() - 1) pw.println(",");
                pw.println();
            }
            pw.println(" ]");
            pw.println("}");
        }
    }

    private static String jsonString(String s) {
        if (s == null || s.isEmpty()) return "null";
        s = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + s + "\"";
    }

    private static String jsonDouble(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "null";
        return Double.toString(v);
    }
}
