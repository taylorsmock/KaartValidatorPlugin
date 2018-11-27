package com.kaartgroup.kaartvalidator.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class Maxspeed extends Test {
    private static final int MAXSPEEDCODE = 4100;
    public static final int MAXSPEED_BLANKSPOT = MAXSPEEDCODE + 1;

    public static final int MAXLENGTH = 30; //meters
    private List<Way> ways;
    public Maxspeed() {
        super(tr("Maxspeed consistency"), tr("Looks for short ways that have the same maxspeed on both sides"));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        ways = new LinkedList<>();
    }

    @Override
    public void endTest() {
        Way pWay = null;
        try {
            for (Way way : ways) {
                pWay = way;
                checkMaxspeedConsistency(way);
            }
        } catch (Exception e) {
            if (pWay != null) {
                System.out.printf("Way https://osm.org/way/%d caused an error" + System.lineSeparator(), pWay.getOsmId());
            }
            e.printStackTrace();
        }
        ways = null;
        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable()) return;
        int connections = getNumberOfConnections(way, "highway", ".*(motorway|trunk|primary|secondary|tertiary|unclassified|residential|service|_link).*");
        if (connections == 2) ways.add(way);
    }

    private int getNumberOfConnections(Way way, String key, String regex) {
        int returnValue = 0;
        if (way.firstNode().isOutsideDownloadArea() || way.lastNode().isOutsideDownloadArea()) return -1;
        for (int i = 0; i < way.getNodesCount(); i++) {
            List<Way> refs = way.getNode(i).getParentWays();
            Boolean connected = false;
            refs.remove(way);
            for (Way wp : refs) {
                if (!wp.hasKey(key)) continue;
                if (wp.get(key).matches(regex)) {
                    connected = true;
                    break;
                }
            }
            if (connected) returnValue++;
        }
        return returnValue;
    }

    private String getMaxspeedOther(Way way, Node node) {
        List<Way> refs = node.getParentWays();
        refs.remove(way);
        Way nWay = null;
        for (Way i : refs) {
            if (i.hasKey("name") && i.get("name").equals(way.get("name"))) {
                nWay = i;
                break;
            } else if (i.hasKey("ref") && i.get("ref").equals(way.get("ref"))) {
                nWay = i;
                break;
            }
        }
        if (nWay != null && nWay.hasKey("maxspeed")) {
            return nWay.get("maxspeed");
        } else {
            return null;
        }
    }

    public void checkMaxspeedConsistency(Way way) {
        String firstNode = getMaxspeedOther(way, way.firstNode());
        String lastNode = getMaxspeedOther(way, way.lastNode());
        Boolean needsMaxspeed = true;
        if (way.hasKey("maxspeed")) needsMaxspeed = false;
        if (firstNode != null && lastNode != null && firstNode.equals(lastNode) && needsMaxspeed) {
            TestError.Builder testError = TestError.builder(this, Severity.WARNING, MAXSPEED_BLANKSPOT)
                    .message(tr("Maxspeed has a blank spot with equal maxspeeds on either side"))
                    .primitives(way);
            if (way.getLength() < MAXLENGTH) {
                testError.fix(() -> new ChangePropertyCommand(way, "maxspeed", firstNode));
            }
            errors.add(testError.build());
        }
    }
}
