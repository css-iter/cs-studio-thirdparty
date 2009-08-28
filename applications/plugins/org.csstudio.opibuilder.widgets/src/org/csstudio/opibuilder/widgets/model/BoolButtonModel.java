package org.csstudio.opibuilder.widgets.model;

import org.csstudio.opibuilder.properties.BooleanProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.eclipse.swt.graphics.RGB;


/**
 * The widget model for Boolean Button.
 * @author Xihui Chen
 *
 */
public class BoolButtonModel extends AbstractBoolControlModel {

	
	
	/** The ID of the effect 3D property. */
	public static final String PROP_EFFECT3D = "effect3D"; //$NON-NLS-1$
	
	/** The ID of the square LED property. */
	public static final String PROP_SQUARE_BUTTON = "squareButton"; //$NON-NLS-1$
	
	/** The ID of the show LED property. */
	public static final String PROP_SHOW_LED = "showLED"; //$NON-NLS-1$
	
	/** The default value of the height property. */	
	private static final int DEFAULT_HEIGHT = 50;
	
	/** The default value of the width property. */
	private static final int DEFAULT_WIDTH = 100;
	
	private static final RGB DEFAULT_FORE_COLOR = CustomMediaFactory.COLOR_BLACK;
	
	public BoolButtonModel() {
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		setForegroundColor(DEFAULT_FORE_COLOR);
	}
	
	@Override
	protected void configureProperties() {
		super.configureProperties();
		
		addProperty(new BooleanProperty(PROP_EFFECT3D, "3D Effect", 
				WidgetPropertyCategory.Display, true));
		
		addProperty(new BooleanProperty(PROP_SQUARE_BUTTON, "Square Button", 
				WidgetPropertyCategory.Display, false));
		
		addProperty(new BooleanProperty(PROP_SHOW_LED, "Show LED", 
				WidgetPropertyCategory.Display, true));
		//setPropertyDescription(PROP_PVNAME, "Readback PV");
	}
	/**
	 * The ID of this widget model.
	 */
	public static final String ID = "org.csstudio.opibuilder.widgets.BoolButton"; //$NON-NLS-1$	
	
	@Override
	public String getTypeID() {
		return ID;
	}

	/**
	 * @return true if the widget would be painted with 3D effect, false otherwise
	 */
	public boolean isEffect3D() {
		return (Boolean) getProperty(PROP_EFFECT3D).getPropertyValue();
	}
	
	/**
	 * @return true if the button is square, false otherwise
	 */
	public boolean isSquareButton() {
		return (Boolean) getProperty(PROP_SQUARE_BUTTON).getPropertyValue();
	}
	
	/**
	 * @return true if the LED should be shown, false otherwise
	 */
	public boolean isShowLED() {
		return (Boolean) getProperty(PROP_SHOW_LED).getPropertyValue();
	}
}
