package fiji.plugin.trackmate.gui.panels.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.text.Document;

public class JNumericTextField extends JTextField {

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1L;
	private static final Border BORDER_FOCUSED = new LineBorder(new Color(252, 117, 0), 1, true);
	private static final Border BORDER_UNFOCUSED = new LineBorder(new Color(150, 150, 150), 1, true);
	private final ActionListener al = new ActionListener() {

		@Override
		public void actionPerformed(final ActionEvent e) {
			checkInput();
		}
	};
	private double value = 0;
	private double oldValue = 0;


	/*
	 * CONSTRUCTORS
	 */

	public JNumericTextField(final Document doc, final String text, final int columns) {
		super(doc, text, columns);
		setBorder(BORDER_UNFOCUSED);
		if (text != null) {
			try {
				value = Double.parseDouble(text);
				oldValue = value;
			} catch (final NumberFormatException nfe) {
				oldValue = 0;
				value = 0;
			}
		}
		addActionListener(al);
		addFocusListener(new FocusListener() {
			@Override
			public void focusLost(final FocusEvent e) {
				checkInput();
				setBorder(BORDER_UNFOCUSED);
			}

			@Override
			public void focusGained(final FocusEvent e) {
				setBorder(BORDER_FOCUSED);
			}
		});
	}


	public JNumericTextField(final int columns) {
		this(null, null, columns);
	}


	public JNumericTextField(final String text, final int columns) {
		this(null, text, columns);
	}


	public JNumericTextField(final String text) {
		this(null, text, 0);
	}

	public JNumericTextField() {
		this(null, null, 0);
	}

	public JNumericTextField(final double value) {
		this( "" + value );
	}

	public double getValue() {
		checkInput();
		return value;
	}

	/*
	 * METHODS
	 */

	private void checkInput() {
		final String str = getText();
		try {
			value = Double.parseDouble(str);
			oldValue = value;
		} catch (final NumberFormatException nfe) {
			value = oldValue;
			setText( "" + value );
		}

	}


}
