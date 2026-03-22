package rbtree;

import java.io.*;
import java.util.*;

public class CSVLoader {
    public static List<Planet> load(String filePath) throws IOException {
        List<Planet> planets = new ArrayList<>();
        int skippedNoPeriod = 0;
        int skippedNonDefault = 0;
        int skippedParseError = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean headerFound = false;

            int iName = -1, iHost = -1, iDefaultFlag = -1;
            int iMethod = -1, iYear = -1, iFacility = -1;
            int iPeriod = -1, iSMA = -1, iEcc = -1;
            int iRade = -1, iMassE = -1, iMassJ = -1, iInsol = -1, iEqt = -1;
            int iSpecType = -1, iTemp = -1, iStRad = -1, iStMass = -1, iStMet = -1;
            int iRa = -1, iDec = -1, iDist = -1;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;

                String[] cols = splitCSV(line);

                if (!headerFound) {
                    headerFound = true;
                    for (int i = 0; i < cols.length; i++) {
                        // These must be EXACT to the CSV.
                        switch (cols[i].trim().toLowerCase()) {
                            case "pl_name": iName = i; break;
                            case "hostname":  iHost = i; break;
                            case "default_flag": iDefaultFlag = i; break;
                            case "discoverymethod": iMethod = i; break;
                            case "disc_year": iYear = i; break;
                            case "disc_facility": iFacility = i; break;
                            case "pl_orbper": iPeriod = i; break;
                            case "pl_orbsmax": iSMA = i; break;
                            case "pl_orbeccen": iEcc = i; break;
                            case "pl_rade": iRade = i; break;
                            case "pl_bmasse": iMassE = i; break;
                            case "pl_bmassj": iMassJ = i; break;
                            case "pl_insol": iInsol = i; break;
                            case "pl_eqt": iEqt = i; break;
                            case "st_spectype": iSpecType = i; break;
                            case "st_teff": iTemp = i; break;
                            case "st_rad": iStRad = i; break;
                            case "st_mass": iStMass = i; break;
                            case "st_met": iStMet = i; break;
                            case "ra": iRa = i; break;
                            case "dec": iDec = i; break;
                            case "sy_dist": iDist = i; break;
                        }
                    }
                    if (iName < 0 || iPeriod < 0)
                        throw new IOException("CSV missing required columns 'pl_name' or 'pl_orbper'.");
                    continue;
                }
                try {
                    if (iDefaultFlag >= 0 && !get(cols, iDefaultFlag).trim().equals("1")) {
                        skippedNonDefault++; continue;
                    }

                    String periodStr = get(cols, iPeriod).trim();
                    if (periodStr.isEmpty()) { skippedParseError++; continue; }

                    double period = Double.parseDouble(periodStr);
                    if (period <= 0) { skippedParseError++; continue; }

                    planets.add(new Planet(
                            get(cols, iName).trim(),
                            get(cols, iHost).trim(),
                            get(cols, iMethod).trim(),
                            parseInt(get(cols, iYear)),
                            get(cols, iFacility).trim(),
                            period,
                            parseDouble(get(cols, iSMA)),
                            parseDouble(get(cols, iEcc)),
                            parseDouble(get(cols, iRade)),
                            parseDouble(get(cols, iMassE)),
                            parseDouble(get(cols, iMassJ)),
                            parseDouble(get(cols, iInsol)),
                            parseDouble(get(cols, iEqt)),
                            stripHTML(get(cols, iSpecType)).trim(),
                            parseDouble(get(cols, iTemp)),
                            parseDouble(get(cols, iStRad)),
                            parseDouble(get(cols, iStMass)),
                            parseDouble(get(cols, iStMet)),
                            parseDouble(get(cols, iRa)),
                            parseDouble(get(cols, iDec)),
                            parseDouble(get(cols, iDist))
                    ));

                } catch (NumberFormatException e) {
                    skippedParseError++;
                }
            }
        }

        System.out.printf(
                "Loaded %,d planets | skipped %,d non-default rows | " +
                        "%,d missing period | %,d parse errors%n",
                planets.size(), skippedNonDefault, skippedNoPeriod, skippedParseError
        );

        return planets;
    }

    private static String get(String[] cols, int i) {
        if (i < 0 || i >= cols.length) return "";
        return cols[i];
    }

    private static double parseDouble(String s) {
        s = s.trim();
        if (s.isEmpty()) return Double.NaN;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return Double.NaN; }
    }

    private static int parseInt(String s) {
        s = s.trim();
        if (s.isEmpty()) return 0;
        try { return (int) Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private static String stripHTML(String s) {
        return s.replaceAll("<[^>]*>", "").trim();
    }

    private static String[] splitCSV(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            }
            else if (c == ',' && !inQuote) {
                tokens.add(sb.toString());
                sb.setLength(0);
            }
            else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
