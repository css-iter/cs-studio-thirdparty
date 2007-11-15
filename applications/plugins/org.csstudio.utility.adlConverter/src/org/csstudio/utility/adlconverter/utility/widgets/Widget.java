/*
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
/*
 * $Id$
 */
package org.csstudio.utility.adlconverter.utility.widgets;

import org.csstudio.sds.importer.AbstractDisplayImporter;
import org.csstudio.sds.model.AbstractWidgetModel;
import org.csstudio.sds.model.DisplayModel;
import org.csstudio.utility.adlconverter.utility.ADLWidget;
import org.csstudio.utility.adlconverter.utility.WrongADLFormatException;
import org.csstudio.utility.adlconverter.utility.widgetparts.ADLBasicAttribute;
import org.csstudio.utility.adlconverter.utility.widgetparts.ADLControl;
import org.csstudio.utility.adlconverter.utility.widgetparts.ADLDynamicAttribute;
import org.csstudio.utility.adlconverter.utility.widgetparts.ADLMonitor;
import org.csstudio.utility.adlconverter.utility.widgetparts.ADLObject;
import org.csstudio.utility.adlconverter.utility.widgetparts.ADLPoints;
import org.csstudio.utility.adlconverter.utility.widgetparts.ADLSensitive;
import org.eclipse.core.runtime.IPath;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 12.09.2007
 */
public abstract class Widget extends AbstractDisplayImporter{
    /** The ADL Widget element as CSS-SDS element.*/
    protected AbstractWidgetModel _widget;
    /** The Widget object parameter. */
    private ADLObject _object = null;
    /** The Widget Basic Attribute . */
    private ADLBasicAttribute _basicAttribute = null ;
    /** The Widget Dynamic Attribute . */
    private ADLDynamicAttribute _dynamicAttribute = null;
    /** The Widget Pointslist. */
    private ADLPoints _points = null;
    /** The Widget monitor Attribute. */
    private ADLMonitor _monitor;
    /** The Widget control Attribute. */
    private ADLControl _control;
    /** The Number of this object in the Display. useful for Debugging and Error handling.*/
    private int _objectNr;
    private ADLSensitive _sensitive;
    
    /**
     * @param widget ADLWidget that describe the Widget.
     */
    public Widget(final ADLWidget widget) {
        setDefaults();
        _objectNr = widget.getObjectNr();
        try {
            makeObject(widget);
        } catch (WrongADLFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * @param widget
     * @param root
     */
    public Widget(ADLWidget widget, DisplayModel root) {
        _widget=root;
        _objectNr = widget.getObjectNr();
        try {
            makeObject(widget);
        } catch (WrongADLFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Set default values.
     */
    private void setDefaults() {
        setWidgetType();
        _widget.setLayer("");
        _widget.setEnabled(true);
    }

    /**
     * Set the Type of Widget.
     */
    abstract void setWidgetType();

    /**
     * Generate the XML-Element.
     */
    private void makeElemnet() {

        _widget.setVisible(true);
        /**
         * Check for dynamic Attribute.
         */
        if((_basicAttribute!=null&&_dynamicAttribute!=null)){ 
            if(_dynamicAttribute.isBoolean()){
                _widget.setDynamicsDescriptor(AbstractWidgetModel.PROP_VISIBILITY, _dynamicAttribute.getBooleanAdlDynamicAttributes());
            }
            if(_dynamicAttribute.isColor()){
                _widget.setDynamicsDescriptor(AbstractWidgetModel.PROP_COLOR_FOREGROUND, _dynamicAttribute.getColorAdlDynamicAttributes());
                if(getControl()==null&&getMonitor()==null){
                    _widget.setAliasValue("channel", "$param$");
                    _widget.setPrimarPv("$param$");
                }
            }
        }
    }

    /**
     * Scan the ADLWidget Object.
     * 
     * @param widget The ADLWidget to generate the XML-Element.
     * @throws WrongADLFormatException WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    private void makeObject(final ADLWidget widget) throws WrongADLFormatException {
        for (ADLWidget obj : widget.getObjects()) {
            if(obj.isType("object")){
                _object = new ADLObject(obj, _widget);
            }else if(obj.isType("\"basic attribute\"")){
                _basicAttribute = new ADLBasicAttribute(obj,_widget);
            }else if(obj.isType("\"dynamic attribute\"")){
                _dynamicAttribute = new ADLDynamicAttribute(obj,_widget);
            }else if(obj.isType("points")){
                _points = new ADLPoints(obj,_widget);
            }else if(obj.isType("monitor")){
                _monitor = new ADLMonitor(obj,_widget);
            }else if(obj.isType("control")){
                _control = new ADLControl(obj, _widget);
            }else if(obj.isType("sensitive")){
                _sensitive = new ADLSensitive(obj, _widget);
            } // else{} polygon have no Parameter
        }
    }
    
    /**
     * @return the WidgetModel
     */
    public AbstractWidgetModel getElement() {
        makeElemnet();
        return _widget;
    }


    /**
     * 
     * @return the Widgetpart {@link ADLObject} of this Widget.
     */
    public ADLObject getObject() {
        return _object;
    }

    /**
     * 
     * @return the Widgetpart {@link ADLBasicAttribute} of this Widget.
     */
    public ADLBasicAttribute getBasicAttribute() {
        return _basicAttribute;
    }

    /**
     * 
     * @return the Widgetpart {@link ADLDynamicAttribute} of this Widget.
     */
    public ADLDynamicAttribute getDynamicAttribute() {
        return _dynamicAttribute;
    }

    /**
     * 
     * @return the Widgetpart {@link ADLPoints} of this Widget.
     */
    public ADLPoints getPoints() {
        return _points;
    }
    /**
     * 
     * @return the Widgetpart {@link ADLMonitor} of this Widget.
     */
    public ADLMonitor getMonitor() {
        return _monitor;
    }

    /**
     * 
     * @return the Widgetpart {@link ADLControl} of this Widget.
     */
    public ADLControl getControl() {
        return _control;
    }

    /**
     * 
     * @return the Widgetpart {@link ADLSensitive} of this Widget.
     */
    public ADLSensitive getSensitive() {
        return _sensitive;
    }

    /**
     * Convert the absolute coordinate to the relative coordinate of this Widget. 
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public void convertCoordinate(final String x, final String y){
        int iX = Integer.parseInt(x);
        int iY = Integer.parseInt(y);
        convertCoordinate(iX, iY);
    }

    /**
     * Convert the absolute coordinate to the relative coordinate of this Widget. 
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    protected void convertCoordinate(final int x, final int y) {
        getObject().setX(getObject().getX()-x);
        getObject().setY(getObject().getY()-y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean importDisplay(final String sourceFile, final IPath targetProject,
            final String targetFileName) throws Exception {
        // Do nothing.
        return false;
    }

}
