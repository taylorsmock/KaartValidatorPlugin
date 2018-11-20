// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.trc;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditorHooks;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionGroup;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import com.kaartgroup.kaartvalidator.validation.TurnLanes;
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
	}
}
