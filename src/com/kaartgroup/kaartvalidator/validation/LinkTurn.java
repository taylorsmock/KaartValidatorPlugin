package com.kaartgroup.kaartvalidator.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;

public class LinkTurn extends Test {
    private static final int LINKTURNCODE = 5000;
    public static final int LINK_ROAD_NO_TURN_RESTRICTION = LINKTURNCODE + 0;

    private List<Way> links;

    public LinkTurn() {
        super(tr("Turn Links"), tr("Find roads with links that do not have a turn restriction"));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        links = new LinkedList<>();
    }

    @Override
    public void endTest() {
        Way pWay = null;
        try {
            for (Way link : links) {
                pWay = link;
                checkLinkAndIntersection(link);
            }
        } catch (Exception e) {
            if (pWay != null) {
                System.out.printf("Way https://osm.org/way/%d caused an error" + System.lineSeparator(), pWay.getOsmId());
            }
            e.printStackTrace();
        }
        links = null;
        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable() || way.firstNode().isOutsideDownloadArea() || way.lastNode().isOutsideDownloadArea()) return;
        if (way.hasKey("highway") && way.get("highway").contains("_link")) {
            links.add(way);
        }
    }

    public void checkLinkAndIntersection(Way link) {
        if (!link.hasKey("highway") || !link.get("highway").matches("^.*_link$")) {
            return;
        }
        List<Way> roads = new LinkedList<>();
        for (Way ref : link.firstNode().getParentWays()) {
            if (ref.equals(link)|| !ref.hasKey("highway") || !ref.get("highway").matches("^(motorway|trunk|primary|secondary|tertiary|unclassified|residential)$")) continue;
            for (Node node : ref.getNodes()) {
                if (node.equals(link.firstNode())) continue;
                for (Way ref2 : node.getParentWays()) {
                    if (ref2.equals(ref) || !ref2.hasKey("highway") || !ref2.get("highway").matches("^(motorway|trunk|primary|secondary|tertiary|unclassified|residential)$")) continue;
                    for (Node node2 : ref2.getNodes()) {
                        if (node.equals(node2)) continue;
                        if (node2.getParentWays().contains(link)) {
                            roads.add(ref);
                            roads.add(ref2);
                            break;
                        }
                    }
                    if (roads.size() == 2) break;
                }
                if (roads.size() == 2) break;
            }
            if (roads.size() == 2) break;
        }
        if (roads.size() != 2) return;
        Boolean noTurnRestriction = true;
        List<String> valid_restrictions = new LinkedList<>();
        if (RightAndLefthandTraffic.isRightHandTraffic(roads.get(0).firstNode().getCoor())) {
            valid_restrictions.add("no_right_turn");
            valid_restrictions.add("only_left_turn");
        } else {
            valid_restrictions.add("no_left_turn");
            valid_restrictions.add("only_right_turn");
        }
        valid_restrictions.add("only_straight_on");
        for (Relation rel : Way.getParentRelations(roads)) {
            if (!"restriction".equals(rel.get("type"))) continue;
            if (!rel.hasKey("restriction")) continue;
            if (valid_restrictions.contains(rel.get("restriction"))) {
                noTurnRestriction = false;
                break;
            }
        }

        if (noTurnRestriction) {
            roads.add(link);
            TestError.Builder testError = TestError.builder(this, Severity.WARNING, LINK_ROAD_NO_TURN_RESTRICTION)
                    .primitives(roads)
                    .message(tr("kaart"), tr("Link connects two roads without a turn restriction"));
            Node node = null;
            if (roads.get(0).isFirstLastNode(roads.get(1).lastNode())) node = roads.get(1).lastNode();
            else if (roads.get(0).isFirstLastNode(roads.get(1).firstNode())) node = roads.get(1).firstNode();
            if (node != null) {
                if (roads.get(1).hasKey("oneway") && roads.get(1).get("oneway") != "no"
                        && roads.get(1).lastNode(true) == node) return;
                final Node node1 = node;
                testError.fix(() -> fixErrorByCreatingTurnRestriction(roads.get(0), node1, roads.get(1)));
            }
            errors.add(testError.build());
        }
    }

    protected static Command fixErrorByCreatingTurnRestriction(Way from, Node via, Way to) {
        if (!from.isFirstLastNode(via) || !to.isFirstLastNode(via)) return null;

        Map<String, String> keys = new HashMap<>();
        keys.put("type", "restriction");
        if (RightAndLefthandTraffic.isRightHandTraffic(via.getCoor())) keys.put("restriction", "no_right_turn");
        else keys.put("restriction", "no_left_turn");

        Relation relation = new Relation();

        relation.addMember(new RelationMember("from", from));
        relation.addMember(new RelationMember("via", via));
        relation.addMember(new RelationMember("to", to));
        relation.setKeys(keys);
        return new AddCommand(from.getDataSet(), relation);
    }
}
