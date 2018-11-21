// License: GPL. For details, see LICENSE file.
package com.kaartgroup.kaartvalidator;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

import com.kaartgroup.kaartvalidator.utils.KaartProperties;


public class KaartPluginPreferences extends DefaultTabPreferenceSetting {
	private final JCheckBox checkturnlaneintersections;
	public KaartPluginPreferences() {
		super("Kaart-Logo-2", tr("KaartPlugin Settings"), tr("Kaart Plugin Settings"));
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
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
		
		markCheckBoxes();

		mainPanel.add(new JLabel("<html><b>Validators :</b> (this is a placeholder)</html>"), GBC.eol().fill(GBC.HORIZONTAL));

		mainPanel.add(checkturnlaneintersections);
		
		createPreferenceTabWithScrollPane(gui, mainPanel);
	}

	private void markCheckBoxes() {
		checkturnlaneintersections.setSelected(KaartProperties.CHECK_TURN_LANES_AT_INTERSECTIONS.get());
	}
}
