// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator;

import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import com.kaartgroup.kaartvalidator.validation.*;

/**
 * Primary class of the Kaart plugin
 * @author Taylor Smock
 */
public class KaartPlugin extends Plugin {
    /**
     * Primary constructor
     */
    public KaartPlugin(PluginInformation info) {
        super(info);
        OsmValidator.addTest(TurnLanes.class);
        OsmValidator.addTest(RoadEndsWithLinks.class);
        OsmValidator.addTest(NameRefConsistency.class);
        OsmValidator.addTest(Maxspeed.class);
        OsmValidator.addTest(LinkDestinations.class);
        OsmValidator.addTest(LinkTurn.class);
        OsmValidator.addTest(Abbreviations.class);
    }

    /*@Override
    public PreferenceSetting getPreferenceSetting() {
        return new KaartPluginPreferences();
    }*/
}
