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
    private List<Way> links;
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
        Way pWay = null;
        try {
            while (ways.size() > 0) {
                links = new LinkedList<>();
                pWay = ways.get(0);
                List<Way> tway = pWay.lastNode().getParentWays();
                Boolean connectsToRoad = false;
                for (Way way : tway) {
                    if (way.hasKey("highway") && !way.get("highway").matches("^.*_link$")) {
                        connectsToRoad = true;
                        break;
                    }
                }
                if (connectsToRoad) {
                    checkDestination(pWay);
                }
                ways.remove(pWay);
                links = null;
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
        if (way.hasKey("highway") && way.get("highway").contains("_link")) {
            ways.add(way);
        }
    }

    /**
     * Checks if there is a destination on the link
     * @param way to check if there is a destination
     * @return true if there is a destination, false otherwise
     */
    public boolean checkDestination(Way way) {
        return this.checkDestination(way, 0);
    }
    /**
     * Checks if there is a destination on the link
     * @param way to check if there is a destination
     * @param recursion the number of times we have recursed into the function already
     * @return true if there is a destination, false otherwise
     */
    protected boolean checkDestination(Way way, int recursion) {
        if (way.lastNode().isOutsideDownloadArea()) return false;
        if (way.hasKey("highway") && way.get("highway").matches("^.*_link$") && !links.contains(way)) {
            links.add(way);
            List<Way> refs = way.firstNode().getParentWays();
            refs.remove(way);
            for (Way ref : refs) {
                if (refs.size() > 1 || recursion >= 100) break;
                if (ref.lastNode() != way.firstNode()) continue;
                if (!checkDestination(ref, recursion + 1)) {
                    if (!links.contains(ref) && ref.hasKey("highway") && ref.get("highway").matches("^.*_link$")) {
                        links.add(ref);
                        ways.remove(ref);
                    }
                }
            }
        }

        Boolean hasDestinationTag = false;
        for (String destinationTag : destinationTags) {
            String wayValue = way.get(destinationTag);
            if (!way.hasKey(destinationTag)) continue;
            else hasDestinationTag = true;
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
        if (recursion == 0) {
            Node lastNode = way.lastNode();
            List<Way> refs = lastNode.getParentWays();
            List<Way> trefs = lastNode.getParentWays();
            trefs.remove(way);
            for (Way ref : refs) {
                if (ref.get("oneway") == "yes" && ref.lastNode().equals(lastNode)) {
                    trefs.remove(ref);
                }
            }
            refs = trefs;
            TestError.Builder testError = TestError.builder(this, Severity.WARNING, DESTINATION_TAG_DOES_NOT_MATCH)
                    .primitives(links)
                    .message(tr("The destination tag does not match or does not exist"));
            Way ref = refs.get(0);
            final List<Way> fLinks = links;
            if (refs.size() == 1 && !hasDestinationTag
                    && ref.hasKey("highway") && !ref.get("highway").matches("^.*_link$")
                    && ref.lastNode() != lastNode) {
                if (ref.hasKey("destination:ref") && !ref.hasKey("ref")) {
                    testError.fix(() -> new ChangePropertyCommand(fLinks, "destination:ref", ref.get("destination:ref")));
                } else if (ref.hasKey("ref") && !ref.hasKey("destination:ref")) {
                    testError.fix(() -> new ChangePropertyCommand(fLinks, "destination:ref", ref.get("ref")));
                } else if (ref.hasKey("destination:street") && !ref.hasKey("name")) {
                    testError.fix(() -> new ChangePropertyCommand(fLinks, "destination:street", ref.get("destination:street")));
                } else if (ref.hasKey("name") && !ref.hasKey("destination:street")) {
                    testError.fix(() -> new ChangePropertyCommand(fLinks, "destination:street", ref.get("name")));
                }
            }
            errors.add(testError.build());
        }
        return false;
    }
}
