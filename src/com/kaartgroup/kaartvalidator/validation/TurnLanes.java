// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator.validation;
//package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Check turn:lanes for errors
 *
 * @author Taylor Smock
 */

public class TurnLanes extends Test {
    public static final int UNCONNECTED_TURN_LANES = 3800;
    public static final int TURN_LANES_DO_NOT_CONTINUE = 3801;
    public static final int TURN_LANES_DO_NOT_END_ON_CONNECTED_WAY = 3802;
    public static final int UNCLEAR_TURN_LANES = 3803;

    private List<Way> turnLaneWays;

    /**
     * Constructor
     */
    public TurnLanes() {
        super(tr("Turn Lanes"), tr("This test checks that turn lanes are correct"));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        turnLaneWays = new LinkedList<>();
    }
    
    @Override
    public void endTest() {
        for (Way p : turnLaneWays) {
            checkConnections(p);
            checkLanesIntersection(p);
        }
        turnLaneWays = null;
        super.endTest();
    }
    /**
     * Check that the proper lanes go through a divided intersection
     * 1) If turn:lanes has a turn onto a street, it should end there
     * 2) If turn:lanes continues through a street, the lanes that are
     *    continuing should continue
     * 3) At the final intersection of the roads, only through turn lanes
     *    should remain
     * We should not check nodes that are outside the downloaded area
     *  | |      |
     * -|-|-    -|-
     *  | |      |
     * -|-|-    -|-
     *  | |      |
     */
    public void checkLanesIntersection(Way p) {
        String oneway = p.get("oneway");
        String name = p.get("name");
        String ref = p.get("ref");
        Boolean turnLanesContinueForward = false;
        Boolean turnLanesContinueBackward = false;
        Way pContinue = null;
        if (oneway == null) { oneway = "no"; }
        Node lastNode = p.getNode(p.getNodesCount() - 1);
        Boolean lastNodeOneway = false;
        Boolean firstNodeOneway = false;
        if (oneway == "yes" || oneway == "no") {
            List<OsmPrimitive> refs = lastNode.getReferrers();
            for (OsmPrimitive wp : refs) {
                if (!(wp instanceof Way)) continue;
                if (wp != p && (name != null && name == wp.get("name") || ref != null && ref == wp.get("ref"))) {
                    pContinue = (Way) wp;
                } else if (wp.get("oneway") == "yes") {
                    lastNodeOneway = true;
                }
            }
            int[] continuingLanes = getContinuingLanes(p, "forward");
            Boolean pContinueTurnlanes = false;
            if (pContinue != null) pContinueTurnlanes = hasTurnLanes(pContinue);
            int pContinueLanes = 0;
            if (pContinue.hasKey("lanes")) pContinueLanes = Integer.parseInt(pContinue.get("lanes"));
            if (lastNodeOneway && (
                    (pContinueTurnlanes && pContinueLanes == continuingLanes[0])
                    || (!pContinueTurnlanes && pContinueLanes == continuingLanes[1] && continuingLanes[2] == 0))) {
                turnLanesContinueForward = true;
            }
            if (!turnLanesContinueForward) {
                errors.add(TestError.builder(this, Severity.WARNING, TURN_LANES_DO_NOT_CONTINUE)
                        .message(tr("Turn lanes do not continue through intersection"))
                        .primitives(p, pContinue)
                        .build());
            }
        }
        if (oneway == "no") {
            Node firstNode = p.getNode(0);
            List<OsmPrimitive> refs = firstNode.getReferrers();
            for (OsmPrimitive wp : refs) {
                if (!(wp instanceof Way)) continue;
                if (wp != p && (name != null && name == wp.get("name") || ref != null && ref == wp.get("ref"))) {
                    pContinue = (Way) wp;
                } else if (wp.get("oneway") == "yes") {
                    firstNodeOneway = true;
                }
            }
            int[] continuingLanes = getContinuingLanes(p, "backward");
            Boolean pContinueTurnlanes = false;
            if (pContinue != null) pContinueTurnlanes = hasTurnLanes(pContinue);
            if (firstNodeOneway && ((pContinueTurnlanes ||
                    continuingLanes[0] != continuingLanes[1])) ||
                    (!pContinueTurnlanes && pContinue.get("lanes") == Integer.toString(continuingLanes[1]))) {
                turnLanesContinueBackward = true;
            }
            if (!turnLanesContinueBackward) {
                errors.add(TestError.builder(this, Severity.WARNING, TURN_LANES_DO_NOT_CONTINUE)
                        .message(tr("Turn lanes do not continue through intersection"))
                        .primitives(p, pContinue)
                        .build());
            }
        }
    }
    
    public void checkConnections(Way p) {
        // Check turn:lanes:backward and turn:lanes:forward
        String turnLanesBackward = p.get("turn:lanes:backward");
        String turnLanesForward = p.get("turn:lanes:forward");
        String turnLanes = p.get("turn:lanes");
        if (turnLanesBackward == null && turnLanesForward == null && turnLanes == null) {
            return;
        }
        int numNodes = p.getNodesCount();
        int numNodesConnected = 0;
        Set<Way> connectedWays = new HashSet<>();
        for (int i = 0; i < numNodes; i++) {
            Node n = p.getNode(i);
            List<OsmPrimitive> refs = n.getReferrers();
            for (OsmPrimitive wp : refs) {
                if (wp != p && wp instanceof Way) {
                   connectedWays.add((Way) wp); 
                }
            }
            if (refs.size() > 1) {
                numNodesConnected++;
            }
        }

        if (numNodesConnected > 2) {
            errors.add(TestError.builder(this, Severity.WARNING, UNCLEAR_TURN_LANES)
                    .message(tr("Road has multiple possibilities for turning"))
                    .primitives(p)
                    .build());
        }
        if (connectedWays.isEmpty()) {
            errors.add(TestError.builder(this, Severity.WARNING, UNCONNECTED_TURN_LANES)
                    .message(tr("Road with turn lanes not connected to anything"))
                    .primitives(p)
                    .build());
        }
    }

    /**
     * Gets the probable number of lanes continuing in a direction
     * @param way The highway with lanes
     * @param direction The direction of travel (forward|backward)
     * @return [lanes continuing through intersection, through lanes only, turn lanes continuing through intersection]
     */
    public int[] getContinuingLanes(Way way, String direction) {
        String turnLanes;
        Node node = null;
        if (way.get("oneway") == "yes") {
            turnLanes = way.get("turn:lanes");
        } else {
            turnLanes = way.get("turn:lanes:" + direction);
        }
        if (direction == "forward" || way.get("oneway") == "yes") {
            node = way.getNode(way.getNodesCount() - 1);
        } else if (direction == "backward") {
            node = way.getNode(0);
        }
        String[] lanes = turnLanes.split("[|]");
        int[] laneBreakdown = getTurnLanes(lanes);
        List<OsmPrimitive> refs = node.getReferrers();
        String directions = null;
        for (OsmPrimitive wp : refs) {
            if (!(wp instanceof Way)) continue;
            Way wpt = (Way) wp;
            if (wpt == way) continue;
            if (way.get("name") != null && way.get("name") != wpt.get("name")
                    || way.get("ref") != null && way.get("ref") != wpt.get("ref")) {
                /* Make certain that the oneway does NOT end on the same node */
                if (wpt.get("oneway") == "yes" && wpt.getNode(wpt.getNodesCount() - 1) != node) {
                    directions = getTurnDirection(way, node, wpt);
                    if (directions != null) break;
                } else if ((!wpt.hasKey("oneway") || wpt.get("oneway") == "no") && wpt.getNode(0) != node
                        && wpt.getNode(wpt.getNodesCount() - 1) != node) {
                    directions = "left|right";
                }
            }
        }
        int returnValue = 0;
        for (int i = 0; i < laneBreakdown.length; i++) {
            returnValue += laneBreakdown[i];
        }
        int turnLanesThrough = 0;
        if (directions != null && directions.contains("left")) {
            returnValue -= laneBreakdown[0];
            turnLanesThrough = laneBreakdown[1] + laneBreakdown[4] + laneBreakdown[5] + laneBreakdown[6];
        }
        if (directions != null && directions.contains("right")) {
            returnValue -= laneBreakdown[1];
            turnLanesThrough = laneBreakdown[0] + laneBreakdown[3] + laneBreakdown[5] + laneBreakdown[6];
        }
        if (directions != null && directions.contains("right") && directions.contains("left")) {
            turnLanesThrough = 0;
        }
        int onlyThrough = laneBreakdown[2] + laneBreakdown[3] + laneBreakdown[4] + laneBreakdown[6];
        return new int[]{returnValue, onlyThrough, turnLanesThrough};
    }

    /**
     * Get the number of turn lanes going in different directions (left|through|right)
     * @param lanes the turn:lanes
     * @return [left, right, through, left;through, right;through, left;right, left;through;right]
     */
    public int[] getTurnLanes(String[] lanes) {
        int left = 0;
        int right = 0;
        int through = 0;
        int leftthrough = 0;
        int rightthrough = 0;
        int leftright = 0;
        int leftthroughright = 0;
        for (String i : lanes) {
            if (i.contains("left") && !i.contains("through") && !i.contains("right")) left++;
            if (!i.contains("left") && !i.contains("through") && i.contains("right")) right++;
            if (!i.contains("left") && i.contains("through") && !i.contains("right")) through++;
            if (i.contains("left") && i.contains("through") && !i.contains("right")) leftthrough++;
            if (!i.contains("left") && i.contains("through") && i.contains("right")) rightthrough++;
            if (i.contains("left") && !i.contains("through") && i.contains("right")) leftright++;
            if (i.contains("left") && i.contains("through") && i.contains("right")) leftthroughright++;
        }
        int[] returnint = {left, right, through, leftthrough, rightthrough, leftright, leftthroughright};
        return returnint;
    }
    
    /**
     * Get the possible directions of turning
     * @param from Initial way
     * @param via Node connecting
     * @param to Final way
     * @return "left", "right", "left|right", null
     */
    public String getTurnDirection(Way from, Node via, Way to) {
        if (!from.containsNode(via) || !to.containsNode(via)) return null;
        if (!to.hasKey("oneway") || to.get("oneway") == "no") return "left|right";
        Node prevFromNode = null;
        Node nextToNode = null;
        if (from.getNode(0) == via) prevFromNode = from.getNode(1);
        else if (from.getNode(from.getNodesCount() - 1) == via) prevFromNode = from.getNode(from.getNodesCount() - 2);
        for (int i = 0; i < to.getNodesCount(); i++) {
            if (to.getNode(i) == via && i != to.getNodesCount() - 1) {
                nextToNode = to.getNode(i);
                break;
            }
        }
        if (nextToNode == null || prevFromNode == null) return null;
        else {
            LatLon tcoord = nextToNode.getCoor();
            LatLon vcoord = via.getCoor();
            LatLon fcoord = prevFromNode.getCoor();
            Double bearing = vcoord.bearing(fcoord) - vcoord.bearing(tcoord);
            // TODO check if right (I just switched left/right)
            if (bearing > 0) return "left";
            else if (bearing < 0) return "right";
            else return null;
        }
    }
    
    @Override
    public void visit(Way way) {
        if (!way.isUsable()) {
            return;
        }
        if (hasTurnLanes(way)) {
            turnLaneWays.add(way);
        }
    }

    public static boolean hasTurnLanes(OsmPrimitive osm) {
        return osm instanceof Way && (osm.hasTag("turn:lanes") || osm.hasTag("turn:lanes:forward") || osm.hasTag("turn:lanes:backward"));
    }
}
