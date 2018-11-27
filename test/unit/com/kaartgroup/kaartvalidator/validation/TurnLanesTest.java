package com.kaartgroup.kaartvalidator.validation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;

import com.kaartgroup.kaartvalidator.AbstractTest;
import com.kaartgroup.kaartvalidator.ImportUtils;

public class TurnLanesTest {
    @Test
    public void getTurnLanesTest() {
        String[] right = {"right"};
        String[] left = {"left"};
        String[] through = {"through"};
        TurnLanes turnLanes = new TurnLanes();/*
        assertTrue(0 == turnLanes.getTurnLanes(right)[0]);
        assertTrue(1 == turnLanes.getTurnLanes(right)[1]);
        assertTrue(0 == turnLanes.getTurnLanes(right)[2]);
        assertTrue(1 == turnLanes.getTurnLanes(left)[0]);
        assertTrue(0 == turnLanes.getTurnLanes(left)[1]);
        assertTrue(0 == turnLanes.getTurnLanes(left)[2]);
        assertTrue(0 == turnLanes.getTurnLanes(through)[0]);
        assertTrue(0 == turnLanes.getTurnLanes(through)[1]);
        assertTrue(1 == turnLanes.getTurnLanes(through)[2]);*/
    }

    @Test
    public void checkConnectionsTest() {
        File file = new File(AbstractTest.TURN_LANES_TO_MANY_POSSIBILITIES);
        DataSet ds = ImportUtils.importOsmFile(file, "testlayer");
        TurnLanes turnLanes = new TurnLanes();
        long id = -39016;
        Way way = (Way) ds.getPrimitiveById(id, OsmPrimitiveType.WAY);
        turnLanes.checkConnections(way);

        //List<TestError> errors = new ArrayList<>();

        //Waychecker wayChecker = new WayChecker(way, turnLanes);

    }
}
