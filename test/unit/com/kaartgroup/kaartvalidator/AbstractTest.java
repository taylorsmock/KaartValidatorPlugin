// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator;

import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * Abstract class for tests that require JOSM's preferences running.
 *
 * @author nokutu, modified
 *
 */
@Ignore
public abstract class AbstractTest {
	public static final String TURN_LANES_TO_MANY_POSSIBILITIES = "test/data/unclear_turn_lanes.osm";
	/**
	 * Initiates the basic parts of JOSM.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		TestUtil.initPlugin();
	}
}
