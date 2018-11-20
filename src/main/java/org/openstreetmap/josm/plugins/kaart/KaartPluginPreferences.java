// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

import com.kaartgroup.kaartvalidator.KaartProperties;


public class KaartPluginPreferences extends DefaultTabPreferenceSetting {
	private final JCheckBox checkturnlaneintersections;
	public KaartPluginPreferences() {
		super("bus", tr("KaartPlugin Settings"), tr("Kaart Plugin Settings"));
		checkturnlaneintersections = new JCheckBox(tr("Check turn lanes going through intersections"));	
	}
	@Override
	public boolean ok() {
		KaartProperties.CHECK_TURN_LANES_AT_INTERSECTIONS.put(this.checkturnlaneintersections.isSelected());
		
		return false;
	}
	@Override
	public void addGui(PreferenceTabbedPane gui) {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 30, 40));
		
		markCheckBoxes();

		mainPanel.add(new JLabel("<html><b>Validator</b>: </html>"), GBC.eol().fill(GBC.HORIZONTAL));
		mainPanel.add(checkturnlaneintersections);
	}

	private void markCheckBoxes() {
		checkturnlaneintersections.setSelected(KaartProperties.CHECK_TURN_LANES_AT_INTERSECTIONS.get());
	}
}
