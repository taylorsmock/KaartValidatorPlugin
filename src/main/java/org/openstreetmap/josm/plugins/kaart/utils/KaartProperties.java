// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator;

import java.util.List;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;

public final class KaartProperties {
	public static final BooleanProperty CHECK_TURN_LANES_AT_INTERSECTIONS = new BooleanProperty("kaartvalidator.check_turn_lanes_at_intersections", false);
	private KaartProperties() {
		// Empty
	}
}
