// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator.utils;

import org.openstreetmap.josm.data.preferences.BooleanProperty;

public final class KaartProperties {
	public static final BooleanProperty CHECK_TURN_LANES_AT_INTERSECTIONS = new BooleanProperty("kaartvalidator.check_turn_lanes_at_intersections", false);
	private KaartProperties() {
		// Empty
	}
}
