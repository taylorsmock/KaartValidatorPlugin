package com.kaartgroup.kaartvalidator.validation;

import static org.junit.Assert.*;

import org.junit.Test;

public class TurnLanesTest {
	public final static String UNCLEAR_TURN_LANES = "test/data/unclear_turn_lanes.osm";
	@Test
	public void getTurnLanesTest() {
		String[] right = {"right"};
		String[] left = {"left"};
		String[] through = {"through"};
		TurnLanes turnLanes = new TurnLanes();
		assertTrue(0 == turnLanes.getTurnLanes(right)[0]);
		assertTrue(1 == turnLanes.getTurnLanes(right)[1]);
		assertTrue(0 == turnLanes.getTurnLanes(right)[2]);
		assertTrue(1 == turnLanes.getTurnLanes(left)[0]);
		assertTrue(0 == turnLanes.getTurnLanes(left)[1]);
		assertTrue(0 == turnLanes.getTurnLanes(left)[2]);
		assertTrue(0 == turnLanes.getTurnLanes(through)[0]);
		assertTrue(0 == turnLanes.getTurnLanes(through)[1]);
		assertTrue(1 == turnLanes.getTurnLanes(through)[2]);
	}

}
