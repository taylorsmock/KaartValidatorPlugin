/**
 *
 */
package com.kaartgroup.kaartvalidator.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * @author tsmock
 *
 */
public class Abbreviations extends Test {
    private static final int ABBRCODE = 5100;
    public static final int CONTAINS_ABBREVIATION = ABBRCODE + 0;

    private List<Way> ways;
    private HashMap<String[], ArrayList<String>> abbreviations;
    public Abbreviations() {
        super(tr("Check for abbreviations in road names"), tr("Looks abbreviations such as Str, St, etc."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        ways = new LinkedList<>();
        abbreviations = new HashMap<>();
        addGreekAbbreviations();
    }

    @Override
    public void endTest() {
        Way pWay = null;
        try {
            for (Way way : ways) {
                pWay = way;
                checkForAbbreviations(pWay);
            }
        } catch (Exception e) {
            if (pWay != null) {
                System.out.printf("Way https://osm.org/way/%d caused an error" + System.lineSeparator(), pWay.getOsmId());
            }
            e.printStackTrace();
        }
        ways = null;
        abbreviations = null;
        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable() || !way.hasKey("highway")) {
            return;
        }
        boolean nametags = false;
        for (Tag tag : way.getKeys().getTags()) {
            if (tag.getKey().contains("name")) {
                nametags = true;
                break;
            }
        }
        if (!nametags) return;
        ways.add(way);
    }

    private void addAbbreviations(String abbreviation, String position, String... expansions) {
        if (expansions.length == 0) {
            expansions = new String[] {position};
            position = "unknown";
        }
        String[] key = new String[] {abbreviation, position};
        ArrayList<String> expansion = new ArrayList<String>(Arrays.asList(expansions));
        if (abbreviations.containsKey(key)) {
            for (String tstr : expansion) {
                if (!abbreviations.get(key).contains(tstr)) {
                    abbreviations.get(key).add(tstr);
                }
            }
        } else {
            abbreviations.put(key, expansion);
        }
    }

    /* Greek abbreviations are current as of 2018-12-13 */
    private void addGreekAbbreviations() {
        addAbbreviations("Αγ.", "prefix", "Αγίας", "Αγίου", "Αγίων");
        addAbbreviations("Αφοί", "prefix", "Αδελφοί");
        addAbbreviations("Αφών", "prefix", "Αδελφών");
        addAbbreviations("Αλ.", "unknown", "Αλέξανδρου");
        addAbbreviations("ΑΤΕΙ", "prefix", "Ανώτατο Τεχνολογικό Εκπαιδευτικό Ίδρυμα");
        addAbbreviations("ΑΤ", "prefix", "Αστυνομικό Τμήμα");
        addAbbreviations("Β.", "prefix", "Βασιλέως", "Βασιλίσσης");
        addAbbreviations("Βασ.", "prefix", "Βασιλέως", "Βασιλίσσης");
        addAbbreviations("Γρ.", "unknown", "Γρηγορίου");
        addAbbreviations("Δ.", "prefix", "Δήμος");
        addAbbreviations("ΔΣ", "prefix", "Δημοτικό Σχολείο");
        addAbbreviations("Δημ. Σχ.", "prefix", "Δημοτικό Σχολείο");
        addAbbreviations("Εθν.", "unknown", "Εθνάρχου", "Εθνική", "Εθνικής");
        addAbbreviations("Ελ.", "unknown", "Ελευθέριος", "Ελευθερίου");
        addAbbreviations("ΕΛΤΑ", "prefix", "Ελληνικά Ταχυδρομεία");
        addAbbreviations("Θεσ/νίκης", "unknown", "Θεσσαλονίκης");
        addAbbreviations("Ι.Μ.", "prefix", "Ιερά Μονή");
        addAbbreviations("Ι.Ν.", "prefix", "Ιερός Ναός");
        addAbbreviations("Κτ.", "prefix", "Κτίριο");
        addAbbreviations("Κων/νου", "unknown", "Κωνσταντίνου");
        addAbbreviations("Λ.", "prefix", "Λεωφόρος", "Λίμνη");
        addAbbreviations("Λεωφ.", "prefix", "Λεωφόρος");
        addAbbreviations("Ν.", "prefix", "Νέα", "Νέες", "Νέο", "Νέοι", "Νέος", "Νησί", "Νομός");
        addAbbreviations("Όρ.", "prefix", "Όρος");
        addAbbreviations("Π.", "prefix", "Παλαιά", "Παλαιές", "Παλαιό", "Παλαιοί", "Παλαιός");
        addAbbreviations("Π.", "unknown", "Ποταμός");
        addAbbreviations("ΑΕΙ", "prefix", "Πανεπιστήμιο");
        addAbbreviations("Παν.", "prefix", "Πανεπιστήμιο");
        addAbbreviations("Πλ.", "prefix", "Πλατεία");
        addAbbreviations("Ποτ.", "unknown", "Ποταμός");
        addAbbreviations("Στρ.", "prefix", "Στρατηγού");
        addAbbreviations("ΕΛΤΑ", "prefix", "Ταχυδρομείο");
        addAbbreviations("ΤΕΙ", "prefix", "Τεχνολογικό Εκπαιδευτικό Ίδρυμα");
    }


    protected void checkForAbbreviations(Way way) {
        for (Tag tag : way.getKeys().getTags()) {
            if (tag.toString().contains("name")) {
                process(way, tag.getKey());
            }
        }
    }

    protected void process(Way way, String key) {
        if (!way.hasKey(key)) return;
        String name = way.get(key);
        for (String[] keys : abbreviations.keySet()) {
            String abbreviation = keys[0];
            String position = keys[1];
            Boolean found = false;
            if (position.equals("unknown") && name.contains(abbreviation)) {
                found = true;
            } else if (position.equals("prefix") && (name.startsWith(abbreviation) || name.startsWith(abbreviation.replace(".", "").concat(" ")))) {
                found = true;
            } else if (position.equals("suffix") && (name.endsWith(abbreviation) || name.endsWith(" ".concat(abbreviation.replace(".", ""))))) {
                found = true;
            }
            if (found) {
                foundAbbreviation(way, key, abbreviation, abbreviations.get(keys));
                break;
            }
        }
    }

    protected void foundAbbreviation(Way way, String key, String abbreviation, ArrayList<String> expansions) {
        TestError.Builder testError = TestError.builder(this, Severity.WARNING, CONTAINS_ABBREVIATION)
                .primitives(way)
                .message(tr("kaart"), way.get(key).concat(tr(" is an abbreviation, try expanding to one of the following: ")).concat(expansions.toString()));
        if (expansions.size() == 1 && way.get(key).contains(abbreviation)) {
            testError.fix(() -> new ChangePropertyCommand(way, key, way.get(key).replace(abbreviation, expansions.get(0))));
        }
        errors.add(testError.build());
    }
}
