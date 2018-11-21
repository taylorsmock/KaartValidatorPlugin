// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator.validation;
//package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
    protected static final int UNCONNECTED_TURN_LANES = 3800;
    protected static final int TURN_LANES_DO_NOT_CONTINUE = 3801;
    protected static final int TURN_LANES_DO_NOT_END_ON_CONNECTED_WAY = 3802;
    protected static final int UNCLEAR_TURN_LANES = 3803;

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
        checkLanesIntersection();
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
    protected void checkLanesIntersection() {
        Area downloadedArea = null;
        for (Way p : turnLaneWays) {
            if (downloadedArea == null) {
                downloadedArea = p.getDataSet().getDataSourceArea();
            }
            // Only check if we are in the downloaded area
            //if (p.isOutsideDownloadArea()) { return; }
            String oneway = p.get("oneway");
            if (oneway == null) { oneway = "no"; }
            if (oneway == "no") {
                // Check turn:lanes:backward and turn:lanes:forward
                String turnLanesBackward = p.get("turnLanesBackward");
                String turnLanesForward = p.get("turnLanesForward");
                if (turnLanesBackward == null && turnLanesForward == null) {
                    return;
                }
                // Check if the connected way(s) is a oneway
                int numNodes = p.getNodesCount();
                int numNodesConnected = 0;
                Set<Way> connectedWays = new HashSet<>();
                for (int i = 0; i < numNodes; i++) {
                    Node n = p.getNode(i);
                    List<OsmPrimitive> refs = n.getReferrers();
                    for (OsmPrimitive wp : refs) {
                        if (wp != p && wp.get("oneway") == "yes") {
                           connectedWays.add((Way) wp); 
                        }
                    }
                    if (refs.size() > 1) {
                        numNodesConnected++;
                    }
                }

                if (connectedWays.size() > 2 || numNodesConnected > 2) {
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
            } else if (oneway == "yes") {
                // Check turn:lanes
            }
        }
    }

    @Override
    public void visit(Way way) {
	System.out.printf("Checking ways");
        if (!way.isUsable()) {
            return;
        }
        if (hasTurnLanes(way)) {
            turnLaneWays.add(way);
        }
    }

    private static boolean hasTurnLanes(OsmPrimitive osm) {
        return osm instanceof Way && (osm.hasTag("turn:lanes") || osm.hasTag("turn:lanes:forward") || osm.hasTag("turn:lanes:backward"));
    }
}
