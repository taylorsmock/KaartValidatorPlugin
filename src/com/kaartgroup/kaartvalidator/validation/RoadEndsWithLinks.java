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
        for (Way p : ways) {
            checkForY(p);
        }
        ways = null;
        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable() || way.hasKey("highway") && way.get("highway").contains("_link")) {
            return;
        }
        ways.add(way);
    }

    private void checkEnd(Way p, Node end) {
        List<OsmPrimitive> refs = end.getReferrers();
        int linkForward = 0;
        int linkBackward = 0;
        List<Way> links = new LinkedList<>();
        for (OsmPrimitive ref : refs) {
            Way way;
            if (!(ref instanceof Way) || ref == p) continue;
            else way = (Way) ref;
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
                            .message(tr("Link goes through road without stopping"))
                            .primitives(p, way)
                            .build());
                }
            }
        }
        if (linkForward + linkBackward == 2 && links.size() >= 2) {
            if (links.get(0).getLength() < MAX_LINK_LENGTH && links.get(1).getLength() < MAX_LINK_LENGTH) {
                errors.add(TestError.builder(this, Severity.WARNING, ROAD_ENDS_WITH_LINKS)
                        .message(tr("Road has two links leaving from the end"))
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
                    .message(tr("Road has multiple links leaving from the end"))
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
