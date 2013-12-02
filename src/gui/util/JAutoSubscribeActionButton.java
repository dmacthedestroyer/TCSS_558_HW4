package gui.util;

import java.awt.event.ActionListener;

import javax.swing.JButton;

public class JAutoSubscribeActionButton extends JButton {
	public JAutoSubscribeActionButton(String buttonText, ActionListener actionListener) {
		super(buttonText);
		addActionListener(actionListener);
	}
}
