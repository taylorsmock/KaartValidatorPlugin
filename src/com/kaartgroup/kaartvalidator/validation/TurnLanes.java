// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator.validation;
//package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.command.ChangePropertyCommand;
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
    private static final int TURNLANESCODE = 3800;
    public static final int UNCONNECTED_TURN_LANES = TURNLANESCODE + 0;
    public static final int TURN_LANES_DO_NOT_CONTINUE = TURNLANESCODE + 1;
    public static final int TURN_LANES_DO_NOT_END_ON_CONNECTED_WAY = TURNLANESCODE + 2;
    public static final int UNCLEAR_TURN_LANES = TURNLANESCODE + 3;
    public static final int LANES_DO_NO_MATCH_AND_NO_TURN_LANES = TURNLANESCODE + 4;
    public static final int NO_TURN_LANES_CHANGING_LANES = TURNLANESCODE + 5;

    public static final int MAXLENGTH = 30; //meters

    private List<Way> turnLaneWays;
    private List<Way> ways;

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
        ways = new LinkedList<>();
    }

    @Override
    public void endTest() {
        Way way = null;
        try {
            for (Way p : turnLaneWays) {
                way = p;
                checkConnections(p);
                checkLanesIntersection(p);
            }
            for (Way p : ways) {
                way = p;
                checkContinuingLanes(p);
            }
        } catch (Exception e) {
            if (way != null) {
                System.out.printf("Way https://osm.org/way/%d caused an error" + System.lineSeparator(), way.getOsmId());
            }
            e.printStackTrace();
        }
        turnLaneWays = null;
        ways = null;
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
        if (oneway == null) { oneway = "no"; }
        if (oneway == "yes" || oneway == "no") {
            checkLanesIntersection(p, "forward");
        }
        if (oneway == "no") {
            checkLanesIntersection(p, "backward");
        }
    }

    /**
     * Make certain that turn:lanes on way in a direction make sense
     * @param p The way to check the turn lanes on
     * @param direction The direction in which to check the turn lanes (forward/backward)
     */
    private void checkLanesIntersection (Way p, String direction) {
        String name = p.get("name");
        String ref = p.get("ref");
        Node node = null;
        if (direction == "forward") node = p.lastNode();
        else if (direction == "backward") node = p.firstNode();
        if (node.isOutsideDownloadArea()) return;
        Way pContinue = null;
        List<Way> refs = node.getParentWays();
        int attachedWays = 0;
        for (Way wp : refs) {
            if (wp != p && (name != null && name == wp.get("name") || ref != null && ref == wp.get("ref"))) {
                pContinue = (Way) wp;
            }
            attachedWays++;
        }
        if (attachedWays == 2) return;
        String[] continuingLanes = getContinuingLanes(p, direction);
        if (continuingLanes == null || pContinue == null) return;
        Boolean continuingLanesOnlyForward = true;
        for (String lane : continuingLanes) {
            if (lane.contains("left") || lane.contains("right") || lane.isEmpty()) {
                continuingLanesOnlyForward = false;
                break;
            }
        }
        if (continuingLanes.length == 0) continuingLanesOnlyForward = false;
        if (continuingLanesOnlyForward) return;
        String[] pContinueLanes = null;
        if (direction == "forward" && !pContinue.hasKey("turn:lanes:" + direction) && pContinue.hasKey("turn:lanes")) {
            pContinueLanes = pContinue.get("turn:lanes").split("[|]");
        } else if (pContinue.hasKey("turn:lanes:" + direction)) {
            pContinueLanes = pContinue.get("turn:lanes:" + direction).split("[|]");
        }
        Boolean doesContinue = true;
        if (pContinueLanes != null && continuingLanes != null && pContinueLanes.length == continuingLanes.length) {
            for (int i = 0; i < pContinueLanes.length; i++) {
                if (pContinueLanes[i].equals(continuingLanes[i])) continue;
                doesContinue = false;
                break;
            }
        } else if ((pContinue.hasKey("lanes") || pContinue.hasKey("lanes:" + direction)) && continuingLanes != null) {
            int lanes;
            if (!pContinue.hasKey("lanes:" + direction)) lanes = Integer.parseInt(pContinue.get("lanes"));
            else lanes = Integer.parseInt(pContinue.get("lanes:" + direction));
            for (String lane : continuingLanes) {
                if (lane.contains("through")) lanes--;
                else {
                    doesContinue = false;
                    break;
                }
            }
            if (lanes != 0) doesContinue = false;
        } else {
            doesContinue = false;
        }
        if (!doesContinue) {
            String key;
            if (direction == "forward" && pContinue.hasKey("oneway") && pContinue.get("oneway").equals("yes")) key = "turn:lanes";
            else key = "turn:lanes:" + direction;
            String continuingLanesValue = "";
            for (int i = 0; i < continuingLanes.length; i++) {
                continuingLanesValue += continuingLanes[i];
                if (i < continuingLanes.length - 1) continuingLanesValue += "|";
            }
            final Way tWay = pContinue;
            final String finalContinuingLanesValue = continuingLanesValue;
            TestError.Builder testError = TestError.builder(this, Severity.WARNING, TURN_LANES_DO_NOT_CONTINUE)
                    .message(tr("Turn lanes do not continue through intersection or do not match up with lanes"))
                    .primitives(p, pContinue);
            int connections = getNumberOfConnections(pContinue, "highway", ".*(motorway|trunk|primary|secondary|tertiary|unclassified|residential|service|_link).*");
            if (connections > 0 && connections <= 2 && tWay.getLength() < MAXLENGTH) {
                testError.fix(() -> new ChangePropertyCommand(tWay, key, finalContinuingLanesValue));
            }
            errors.add(testError.build());
        }
    }

    /**
     * Get the number of connections a way has that have a key with regex
     * @param way
     * @param key
     * @param regex
     * @return
     */
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
    /**
     * Check that a way with turn lanes only has connections to different ways at a maximum of two different nodes
     * @param p Way to check
     */
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
            if (n.isOutsideDownloadArea()) return;
            List<Way> refs = n.getParentWays();
            for (Way wp : refs) {
                if (!wp.hasKey("highway")) continue;
                if (wp.get("highway").matches(".*(motorway|trunk|primary|secondary|tertiary|unclassified|residential|service).*")) {
                    connectedWays.add(wp);
                }
            }
            List<Way> refs2 = n.getParentWays();
            for (Way wp : refs2) {
                if (!connectedWays.contains(wp)) {
                    refs.remove(wp);
                }
            }
            if (refs.size() > 1) {
                numNodesConnected++;
            }
        }
        Boolean connectedTurnLanesForward = false;
        Boolean connectedTurnLanesBackward = false;
        if (turnLanesForward != null || turnLanes != null) {
            List<OsmPrimitive> refs = p.getNode(p.getNodesCount() - 1).getReferrers();
            for (OsmPrimitive wp : refs) {
                if (wp instanceof Way && wp != p) connectedTurnLanesForward = true;
            }
        }
        if (turnLanesBackward != null) {
            List<OsmPrimitive> refs = p.firstNode().getReferrers();
            for (OsmPrimitive wp : refs) {
                if (wp instanceof Way && wp != p) connectedTurnLanesBackward = true;
            }
        }

        if (numNodesConnected > 2) {
            errors.add(TestError.builder(this, Severity.WARNING, UNCLEAR_TURN_LANES)
                    .message(tr("Road has multiple possibilities for turning"))
                    .primitives(p)
                    .build());
        }
        if (connectedWays.isEmpty()
                || (!connectedTurnLanesForward && (turnLanesForward != null || turnLanes != null))
                || (!connectedTurnLanesBackward && (turnLanesBackward != null))) {
            errors.add(TestError.builder(this, Severity.WARNING, UNCONNECTED_TURN_LANES)
                    .message(tr("Road with turn lanes not connected to anything"))
                    .primitives(p)
                    .build());
        }
    }

    /**
     * Gets the probable lanes continuing in a direction
     * @param way The highway with lanes
     * @param direction The direction of travel (forward|backward)
     * @return remaining lanes or null if there are no remaining lanes
     */
    public String[] getContinuingLanes(Way way, String direction) {
        String turnLanes;
        Node node = null;
        if (way.get("oneway") == "yes" && way.hasKey("turn:lanes")) {
            turnLanes = way.get("turn:lanes");
        } else if (way.hasKey("turn:lanes:" + direction)){
            turnLanes = way.get("turn:lanes:" + direction);
        } else {
            return null;
        }
        if (direction == "forward" || way.get("oneway") == "yes") {
            node = way.lastNode();
        } else if (direction == "backward") {
            node = way.firstNode();
        }
        String[] lanes = turnLanes.split("[|]");
        for (int i = 0; i < lanes.length; i++) {
            if (lanes[i].isBlank()) lanes[i] = "through";
        }
        List<Way> refs = node.getParentWays();
        String[] directions = null;
        for (Way wp : refs) {
            if (wp == way) continue;
            if (way.hasKey("name") && !way.get("name").equals(wp.get("name"))
                    || way.hasKey("ref") && !way.get("ref").equals(wp.get("ref"))) {
                directions = getTurnDirection(way, node, wp);
                if (directions != null && directions[0].equals("through")) continue;
                else if (directions != null) break;
            }
        }
        if (directions == null) return null;
        for (String turn : directions) {
            for (int i = 0; i < lanes.length; i++) {
                while (lanes[i].contains(turn)) {
                    lanes[i] = lanes[i].replaceAll(turn, "");
                    lanes[i] = lanes[i].replaceAll("^;|;$", "");
                    lanes[i] = lanes[i].replaceAll(";;", ";");
                }
            }
        }
        List<String> rlanes = new LinkedList<>();
        for (String lane : lanes) {
            if (!lane.isBlank()) rlanes.add(lane);
        }
        String[] array = new String[rlanes.size()];
        return rlanes.toArray(array);
    }

    /**
     * Get the possible directions of turning
     * @param from Initial way
     * @param via Node connecting
     * @param to Final way
     * @return ["left"], ["right"], ["left", "right"], null if they are not connected at Node via
     */
    public String[] getTurnDirection(Way from, Node via, Way to) {
        if (!from.containsNode(via) || !to.containsNode(via)) return null;
        if (!to.hasKey("oneway") || to.get("oneway") == "no") return new String[] {"left", "right"};
        Node prevFromNode = null;
        Node nextToNode = null;
        if (from.firstNode() == via) prevFromNode = from.getNode(1);
        else if (from.lastNode() == via) prevFromNode = from.getNode(from.getNodesCount() - 2);
        for (int i = 0; i < to.getNodesCount(); i++) {
            if (to.getNode(i) == via && i != to.getNodesCount() - 1) {
                nextToNode = to.getNode(i + 1);
                break;
            }
        }
        if (nextToNode == null || prevFromNode == null) return null;
        else {
            LatLon tcoord = nextToNode.getCoor();
            LatLon vcoord = via.getCoor();
            LatLon fcoord = prevFromNode.getCoor();
            Double fbearing = vcoord.bearing(fcoord);
            Double tbearing = vcoord.bearing(tcoord);
            Double bearing = fbearing - tbearing;
            if ((bearing > 0 && bearing < Math.PI) || (bearing < -Math.PI && bearing > -2 * Math.PI)) return new String[] {"right"};
            else if ((bearing < 0 && bearing > -Math.PI) || (bearing > Math.PI && bearing < 2 * Math.PI)) return new String[] {"left"};
            else return new String[] {"through"};
        }
    }

    /**
     * Check a way that connects to another way with the same ref/name has lane change indications
     * @param way The way to check
     * @param key The key (ref/name) that we are interested in
     */
    private void checkContinuingWays(Way way, String key) {
        Way wayContinue = null;
        for (OsmPrimitive ref : way.getNode(way.getNodesCount() - 1).getReferrers()) {
            if (ref instanceof Way && ref != way) {
                if ((ref.hasKey("name") && ref.get("name") == way.get("name"))
                        || (ref.hasKey("ref") && ref.get("ref") == way.get("ref"))) {
                    wayContinue = (Way) ref;
                    break;
                }
            }
        }
        if (wayContinue != null) {
            if ((!wayContinue.hasKey(key) || (wayContinue.hasKey(key) && wayContinue.get(key) != way.get(key)))
                    && (!way.hasKey("turn:lanes") && !way.hasKey("turn:lanes:forward") && !way.hasKey("turn:lanes:backward"))) {
                errors.add(TestError.builder(this, Severity.WARNING, LANES_DO_NO_MATCH_AND_NO_TURN_LANES)
                        .message(tr("There are not turn lanes going into a continuing road with a different number of lanes"))
                        .primitives(way, wayContinue)
                        .build());
            }
        } else if (wayContinue == null) {
            return;
        }
        String keyConcat = "turn:".concat(key);
        String[] turnLanes;
        if (way.hasKey(keyConcat)) turnLanes = way.get(keyConcat).split("[|]");
        else turnLanes = new String[] {"unknown"};
        int possibleAdditionalLanes = 0;
        int possibleRemovedLanes = 0;
        for (String lane : turnLanes) {
            if (lane.contains("slight_left") && lane.contains("through")) possibleAdditionalLanes++;
            if (lane.contains("slight_right") && lane.contains("through")) possibleAdditionalLanes++;
            if (lane.contains("merge_to_right") || lane.contains("merge_to_left")) possibleRemovedLanes++;
        }
        int lanes = Integer.parseInt(way.get(key));
        int lanesContinue = Integer.parseInt(wayContinue.get(key));
        if (lanes != lanesContinue + possibleAdditionalLanes - possibleRemovedLanes
                || possibleAdditionalLanes > 2 || possibleRemovedLanes > 2) {
            errors.add(TestError.builder(this, Severity.WARNING, NO_TURN_LANES_CHANGING_LANES)
                    .message(tr("There is no indication of which lanes change"))
                    .primitives(way, wayContinue)
                    .build());
        }
    }
    /**
     * Check way to ensure that we have indications of which lanes appear/disappear.
     * @param way Way to check
     */
    public void checkContinuingLanes(Way way) {
        if (way.hasKey("turn:lanes:forward")) {
            checkContinuingWays(way, "lanes:forward");
        }
        if (way.hasKey("turn:lanes:backward")) {
            checkContinuingWays(way, "lanes:backward");
        }
        if (!way.hasKey("turn:lanes:backward") && !way.hasKey("turn:lanes:forward") && way.hasKey("turn:lanes")) {
            checkContinuingWays(way, "lanes");
        }
    }
    @Override
    public void visit(Way way) {
        if (!way.isUsable()) {
            return;
        }
        if (hasTurnLanes(way)) {
            turnLaneWays.add(way);
        } else {
            ways.add(way);
        }
    }

    public static boolean hasTurnLanes(OsmPrimitive osm) {
        return osm instanceof Way && (osm.hasTag("turn:lanes") || osm.hasTag("turn:lanes:forward") || osm.hasTag("turn:lanes:backward"));
    }
}
