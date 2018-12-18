/**
 *
 */
package com.kaartgroup.kaartvalidator.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
        addEnglishAbbreviations();
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
            if (tag.getKey().contains("name") && !tag.getKey().equals("int_name")) {
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
        ArrayList<String> expansion = new ArrayList<String>(Arrays.asList(expansions));
        if (!position.equals("unknown") && !position.equals("suffix") && !position.equals("prefix")) {
            expansion.add(position);
            position = "unknown";
        }
        String[] key = new String[] {abbreviation, position};
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

    private void addEnglishAbbreviations() {
        addAbbreviations("Accs", "Access");
        addAbbreviations("AFB", "Air Force Base");
        addAbbreviations("ANGB", "Air National Guard Base");
        addAbbreviations("Aprt", "Airport");
        addAbbreviations("Al", "Alley");
        addAbbreviations("All", "Alley");
        addAbbreviations("Ally", "Alley");
        addAbbreviations("Aly", "Alley");
        addAbbreviations("Alwy", "Alleyway");
        addAbbreviations("Ambl", "Amble");
        addAbbreviations("Apts", "Apartments");
        addAbbreviations("Apch", "Approach");
        addAbbreviations("Arc", "Arcade");
        addAbbreviations("Artl", "Arterial");
        addAbbreviations("Arty", "Artery");
        addAbbreviations("Av", "Avenue");
        addAbbreviations("Ave", "Avenue");
        addAbbreviations("Bk", "Back");
        addAbbreviations("Ba", "Banan");
        addAbbreviations("Basn", "Basin");
        addAbbreviations("Bsn", "Basin");
        addAbbreviations("Bch", "Beach");
        addAbbreviations("Bnd", "Bend");
        addAbbreviations("Blk", "Block");
        addAbbreviations("Bwlk", "Boardwalk");
        addAbbreviations("Blvd", "Boulevard");
        addAbbreviations("Bvd", "Boulevard");
        addAbbreviations("Bdy", "Boundary");
        addAbbreviations("Bl", "Bowl");
        addAbbreviations("Br", "Brace", "Brae", "Bridge");
        addAbbreviations("Brk", "Break");
        addAbbreviations("Bdge", "Bridge");
        addAbbreviations("Bri", "Bridge");
        addAbbreviations("Bdwy", "Broadway");
        addAbbreviations("Bway", "Broadway");
        addAbbreviations("Bwy", "Broadway");
        addAbbreviations("Brk", "Brook");
        addAbbreviations("Brw", "brow");
        addAbbreviations("Bldgs", "Buildings");
        addAbbreviations("Bldngs", "Buildings");
        //addAbbreviations("Bus", "Business");
        addAbbreviations("Bps", "Bypass");
        addAbbreviations("Byp", "Bypass");
        addAbbreviations("Bypa", "Bypass");
        addAbbreviations("Bywy", "Byway");
        addAbbreviations("Cvn", "Caravan");
        //addAbbreviations("Caus", "Causway");
        addAbbreviations("Cswy", "Causeway");
        addAbbreviations("Cway", "Causeway");
        addAbbreviations("Cen", "Center", "Centre");
        addAbbreviations("Ctr", "Center", "Centre");
        addAbbreviations("Ctrl", "Central");
        addAbbreviations("Cnwy", "Centreway");
        addAbbreviations("Ch", "Chase", "Church");
        addAbbreviations("Cir", "Circle");
        addAbbreviations("Cct", "Circuit");
        addAbbreviations("Ci", "Circuit");
        addAbbreviations("Crc", "Circus");
        addAbbreviations("Crcs", "Circus");
        addAbbreviations("Cty", "City");
        addAbbreviations("Cl", "Close");
        addAbbreviations("Cmn", "Common");
        addAbbreviations("Comm", "Common", "Community");
        addAbbreviations("Cnc", "Concourse");
        //addAbbreviations("Con", "Concourse");
        addAbbreviations("Cps", "Copse");
        addAbbreviations("Cnr", "Corner");
        addAbbreviations("Crn", "Corner");
        addAbbreviations("Cso", "Corso");
        addAbbreviations("Cotts", "Cottages");
        //addAbbreviations("Co", "County");
        addAbbreviations("CR", "County Road", "County Route");
        addAbbreviations("Crt", "Court");
        addAbbreviations("Ct", "Court");
        addAbbreviations("Cyd", "Courtyard");
        addAbbreviations("Ctyd", "Courtyard");
        addAbbreviations("Ce", "Cove");
        addAbbreviations("Cov", "Cove");

        // TODO add more
    }


    protected void checkForAbbreviations(Way way) {
        for (Tag tag : way.getKeys().getTags()) {
            if (tag.getKey().contains("name") && !tag.getKey().equals("int_name")) {
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
            if (position.equals("unknown") && (name.matches(".*\\b" + abbreviation + "(\\b|\\.\\b).*")
                    || name.matches(".*\\b" + abbreviation.replace(".", "") + "(\\b|\\.\\b).*"))) {
                found = true;
            } else if (position.equals("prefix") && (name.startsWith(abbreviation) || name.startsWith(abbreviation.replace(".", "").concat(" ")))) {
                found = true;
            } else if (position.equals("suffix") && (name.endsWith(abbreviation) || name.endsWith(" ".concat(abbreviation.replace(".", ""))))) {
                found = true;
            }
            if (found) {
                foundAbbreviation(way, key, abbreviation, position, abbreviations.get(keys));
                break;
            }
        }
    }

    protected int countInstancesOf(String findIn, String find) {
        int index = findIn.indexOf(find);
        int count = 0;
        while (index != -1) {
            count++;
            findIn = findIn.substring(index + 1);
            index = findIn.indexOf(find);
        }
        return count;
    }

    protected void foundAbbreviation(Way way, String key, String abbreviation, String position, ArrayList<String> expansions) {
        HashSet<String> arrayList = new HashSet<>();
        arrayList.addAll(expansions);
        if (!position.equals("unknown")) {
            ArrayList<String> array = abbreviations.get(new String[] {abbreviation, "unknown"});
            if (array != null) {
                arrayList.addAll(array);
            }
        }
        if (!position.equals("prefix")) {
            ArrayList<String> array = abbreviations.get(new String[] {abbreviation, "prefix"});
            if (array != null) {
                arrayList.addAll(array);
            }
        }
        if (!position.equals("postfix")) {
            ArrayList<String> array = abbreviations.get(new String[] {abbreviation, "postfix"});
            if (array != null) {
                arrayList.addAll(array);
            }
        }
        TestError.Builder testError = TestError.builder(this, Severity.WARNING, CONTAINS_ABBREVIATION)
                .primitives(way)
                .message(tr("kaart"), abbreviation.concat(tr(" is an abbreviation in \"")).concat(key).concat(tr("\", try expanding to one of the following: ")).concat(arrayList.toString()));
        if (arrayList.size() == 1 && way.get(key).contains(abbreviation) && countInstancesOf(way.get(key), abbreviation) == 1) {
            String replaceValue = way.get(key);
            replaceValue = replaceValue.replace(abbreviation, expansions.get(0));
            replaceValue = replaceValue.replace(expansions.get(0).concat("."), expansions.get(0));
            final String rv = replaceValue;
            testError.fix(() -> new ChangePropertyCommand(way, key, rv));
        }
        errors.add(testError.build());
    }
}
