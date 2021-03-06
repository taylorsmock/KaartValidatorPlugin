// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Check for roads that end with Y's
 * @author Taylor Smock
 *
 */
public class RoadEndsWithLinks extends Test {
    private static final int ROADENDSWITHLINKSCODE = 3900;
    public final static int ROAD_ENDS_WITH_LINKS = ROADENDSWITHLINKSCODE + 0;
    public final static int ROAD_HAS_LINK_GOING_THROUGH = ROADENDSWITHLINKSCODE + 1;
    public static final int MAX_LINK_LENGTH = 30;

    private List<Way> ways;
    /**
     * Constructor
     */
    public RoadEndsWithLinks() {
        super(tr("Road ends with Links"), tr("Find roads that end with a Y with links"));
    }
    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        ways = new LinkedList<>();
    }

    @Override
    public void endTest() {
        Way way = null;
        try {
            for (Way p : ways) {
                way = p;
                checkForY(p);
            }
        } catch (Exception e) {
            if (way != null ) {
                System.out.printf("Way https://osm.org/way/%d caused an error" + System.lineSeparator(), way.getOsmId());
            }
            e.printStackTrace();
        }
        ways = null;
        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable() || way.hasKey("highway") && !way.get("highway").matches("^(motorway|trunk|primary|secondary|tertiary|unclassified|residential)$")) {
            return;
        }
        ways.add(way);
    }

    private void checkEnd(Way p, Node end) {
        List<Way> refs = end.getParentWays();
        refs.remove(p);
        int linkForward = 0;
        int linkBackward = 0;
        List<Way> links = new LinkedList<>();
        for (Way ref : refs) {
            if (ref.hasKey("highway") && ref.get("highway").contains("_link")) continue;
            if (ref.hasKey("name") && p.hasKey("name") && ref.get("name").equals(p.get("name"))
                    || (ref.hasKey("ref") && p.hasKey("ref") && ref.get("ref").equals(p.get("ref")))) {
                return;
            }
        }
        for (Way ref : refs) {
            Way way = ref;
            if (way.hasKey("highway") && way.get("highway").contains("_link")) {
                if (way.firstNode() == end) {
                    linkForward++;
                    links.add(way);
                }
                else if (way.lastNode() == end) {
                    linkBackward++;
                    links.add(way);
                }
                else {
                    errors.add(TestError.builder(this, Severity.WARNING, ROAD_HAS_LINK_GOING_THROUGH)
                            .message(tr("kaart"), tr("Check for Y junction links (link passes through road)"))
                            .primitives(p, way)
                            .build());
                }
            }
        }
        if (linkForward + linkBackward == 2 && links.size() >= 2) {
            if (links.get(0).getLength() < MAX_LINK_LENGTH && links.get(1).getLength() < MAX_LINK_LENGTH) {
                errors.add(TestError.builder(this, Severity.WARNING, ROAD_ENDS_WITH_LINKS)
                        .message(tr("kaart"), tr("Check for Y junction links (road has two links at the end)"))
                        .primitives(p, links.get(0), links.get(1))
                        .build());
            }
        } else if (links.size() >= 3) {
            OsmPrimitive[] osm = new OsmPrimitive[links.size() + 1];
            for (int i = 0; i < links.size(); i++) {
                osm[i] = links.get(i);
            }
            osm[osm.length - 1] = p;
            errors.add(TestError.builder(this, Severity.WARNING, ROAD_ENDS_WITH_LINKS)
                    .message(tr("kaart"), tr("Check for Y junction links (road has multiple links leaving from the end)"))
                    .primitives(osm)
                    .build());
        }
    }
    public void checkForY(OsmPrimitive p) {
        Way way;
        if (p instanceof Way) way = (Way) p;
        else return;
        checkEnd(way, way.firstNode());
        checkEnd(way, way.lastNode());
    }
}
