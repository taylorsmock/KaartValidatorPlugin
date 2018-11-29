package com.kaartgroup.kaartvalidator.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class NameRefConsistency extends Test {
    private static final int NAMEREFCODE = 3800;
    public static final int NAME_CHANGES = NAMEREFCODE + 0;
    public static final int REF_CHANGES = NAMEREFCODE + 1;

    private List<Way> ways;
    private List<Way> visited;
    public NameRefConsistency() {
        super(tr("Check Name/Ref consistency of roads"), tr("Looks for where road names/refs change"));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        ways = new LinkedList<>();
        visited = new LinkedList<>();
    }

    @Override
    public void endTest() {
        Way pWay = null;
        try {
            for (Way way : ways) {
                pWay = way;
                if (visited.contains(way)) continue;
                check(way, "ref");
                check(way, "name");
            }
        } catch (Exception e) {
            if (pWay != null) {
                System.out.printf("Way https://osm.org/way/%d caused an error" + System.lineSeparator(), pWay.getOsmId());
            }
            e.printStackTrace();
        }
        ways = null;
        visited = null;
        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable() || !way.hasKey("highway")) {
            return;
        }
        ways.add(way);
    }

    private void check(Way way, String key, Node node) {
        if (node.isOutsideDownloadArea() || !way.hasKey(key)) return;
        List<Way> rways = node.getParentWays();
        rways.remove(way);
        if (rways.size() == 0) return;
        List<Way> tmpList = new LinkedList<>();
        Hashtable<String, Integer> names = new Hashtable<String, Integer>();
        for (Way ref : rways) {
            tmpList.add(ref);
            if (ref.firstNode() != node && ref.lastNode() != node) {
                tmpList.remove(ref);
                continue;
            }
            if (!ref.hasKey(key)) continue;
            if (!names.containsKey(ref.get(key)) && ref.get(key) != way.get(key)) {
                names.put(ref.get(key), 1);
            } else if (names.containsKey(ref.get(key)) && ref.get(key) != way.get(key)) {
                names.put(ref.get(key), names.get(ref.get(key)) + 1);
                for (Way ref2 : rways) {
                    if (!ref2.hasKey(key)) continue;
                    if (tmpList.contains(ref2) && names.containsKey(ref2.get(key))) tmpList.remove(ref2);
                }
            }
        }
        rways = tmpList;
        tmpList = null;
        int expectedWays = 0;
        int actualWays = 0;
        if (way.hasKey("oneway") && way.get("oneway").equals("yes")) expectedWays++;
        else if (way != null) expectedWays += 2;
        for (Way i : rways) {
            if (!i.hasKey("highway")) continue;
            if (i.hasKey("oneway") && i.get("oneway").equals("yes") && i.get(key) == way.get(key)) {
                actualWays++;
            } else if (i != null && i.get(key) == way.get(key)) {
                actualWays += 2;
            }
        }
        if ((expectedWays != actualWays && expectedWays + actualWays != 4)
                && rways.size() >= 1) {
            int code = NAMEREFCODE;
            String message = "A key changes";
            if (key == "ref") {
                code = REF_CHANGES;
                message = tr("The ref changes");
            } else if (key == "name") {
                code = NAME_CHANGES;
                message = tr("The name changes");
            }
            rways.add(way);
            OsmPrimitive[] osm = new OsmPrimitive[rways.size()];
            for (int i = 0; i < rways.size(); i++) {
                osm[i] = rways.get(i);
                visited.add((Way) rways.get(i));
            }
            if (osm.length == 0) return;
            errors.add(TestError.builder(this, Severity.WARNING, code)
                    .message(tr("kaart"), tr(message))
                    .primitives(osm)
                    .build());
        }
    }
    public void check(Way way, String key) {
        check(way, key, way.firstNode());
        check(way, key, way.lastNode());
    }
}
