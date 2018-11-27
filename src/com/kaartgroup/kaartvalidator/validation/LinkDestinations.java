package com.kaartgroup.kaartvalidator.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class LinkDestinations extends Test {
    private static final int LINKDESTINATIONSCODE = 4000;
    public static final int DESTINATION_TAG_DOES_NOT_MATCH = LINKDESTINATIONSCODE + 0;

    public static final int MAXLENGTH = 30; //meters

    private List<Way> ways;
    private String[] destinationTags = {"destination:ref", "destination:street"};

    public LinkDestinations() {
        super(tr("Link Destinations"), tr("This test checks link destinations"));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        ways = new LinkedList<>();
    }

    @Override
    public void endTest() {
        for (Way way : ways) {
            checkDestination(way);
        }
        ways = null;
        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable()) return;
        if (way.hasKey("highway") && way.get("highway").contains("_link")) {
            ways.add(way);
        }
    }

    public boolean checkDestination(Way way) {
        for (String destinationTag : destinationTags) {
            String wayValue = way.get(destinationTag);
            if (!way.hasKey(destinationTag)) continue;
            Node lastNode = way.lastNode();
            List<Way> refs = lastNode.getParentWays();
            refs.remove(way);
            for (Way ref : refs) {
                if (ref.hasKey(destinationTag) && ref.get(destinationTag).equals(wayValue)
                        || ref.hasKey("ref") && ref.get("ref").equals(wayValue)
                        || ref.hasKey("name") && ref.get("name").equals(wayValue)) {
                    return true;
                }
            }
        }
        Node lastNode = way.lastNode();
        List<Way> refs = lastNode.getParentWays();
        refs.remove(way);
        TestError.Builder testError = TestError.builder(this, Severity.WARNING, DESTINATION_TAG_DOES_NOT_MATCH)
                .primitives(way)
                .message(tr("The destination tag does not match or does not exist"));
        if (refs.size() == 1 || refs.size() == 2
                && (refs.get(0).lastNode() == refs.get(1).firstNode()
                || refs.get(0).firstNode() == refs.get(1).lastNode())) {
            if (refs.get(0).hasKey("ref")) {
                testError.fix(() -> new ChangePropertyCommand(way, "destination:ref", refs.get(0).get("ref")));
            } else if (refs.get(0).hasKey("name")) {
                testError.fix(() -> new ChangePropertyCommand(way, "destination:street", refs.get(0).get("name")));
            } else if (refs.get(0).hasKey("destination:ref")) {
                testError.fix(() -> new ChangePropertyCommand(way, "destination:ref", refs.get(0).get("destination:ref")));
            } else if (refs.get(0).hasKey("destination:street")) {
                testError.fix(() -> new ChangePropertyCommand(way, "destination:street", refs.get(0).get("destination:street")));
            }
        }
        errors.add(testError.build());
        return false;
    }
}