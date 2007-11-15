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
package org.csstudio.utility.adlconverter.utility.widgetparts;

import org.csstudio.sds.model.AbstractWidgetModel;
import org.csstudio.sds.model.DynamicsDescriptor;
import org.csstudio.sds.model.logic.ParameterDescriptor;
import org.csstudio.utility.adlconverter.utility.ADLHelper;
import org.csstudio.utility.adlconverter.utility.ADLWidget;
import org.csstudio.utility.adlconverter.utility.WrongADLFormatException;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 12.09.2007
 */
public class ADLMonitor extends WidgetPart{

    /**
     * The foreground Color (also Font color).
     */
    private String _clr;
    /**
     * The background Color.
     */
    private String _bclr;
    /**
     * The Channel.
     */
    private String[] _chan;
    /**
     * The Record property/Feldname.
     */
    private String _postfix;

    /**
     * The default constructor.
     * 
     * @param monitor An ADLWidget that correspond a ADL Monitor. 
     * @param parentWidgetModel The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    public ADLMonitor(final ADLWidget monitor, final AbstractWidgetModel parentWidgetModel) throws WrongADLFormatException {
        super(monitor, parentWidgetModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init() {
        /* Not to initialization*/
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    final void parseWidgetPart(final ADLWidget monitor) throws WrongADLFormatException {
        assert !monitor.isType("monitor") : "This "+monitor.getType()+" is not a ADL monitor";

        for (String parameter : monitor.getBody()) {
            if(parameter.trim().startsWith("//")){
                continue;
            }
            String[] row = parameter.split("=");
            if(row.length!=2){
                throw new WrongADLFormatException("This "+parameter+" is a wrong ADL monitor");
            }
            if(row[0].trim().toLowerCase().equals("clr")){
                _clr=row[1].trim();
            }else if(row[0].trim().toLowerCase().equals("bclr")){
                _bclr=row[1].trim();
            }else if(row[0].trim().toLowerCase().equals("chan")){   // chan and rdbk means both the same. Readback channel.
                _chan=ADLHelper.cleanString(row[1]);
            }else if(row[0].trim().toLowerCase().equals("rdbk")){
                _chan=ADLHelper.cleanString(row[1]);
            }else {
                throw new WrongADLFormatException("This "+row[0]+" is a wrong ADL monitor parameter. "+parameter);
            }
        }
    }

    
    /**
     * Generate all Elements from ADL Monitor Attributes.
     */
    @Override
    final void generateElements() {
        if(_clr!=null){
            _widgetModel.setForegroundColor(ADLHelper.getRGB(_clr));
//          /* alarm displayed as foreground color */
            DynamicsDescriptor dynamicsDescriptor = new DynamicsDescriptor("org.css.sds.color.default_epics_alarm_foreground");
            dynamicsDescriptor.addInputChannel(new ParameterDescriptor("$channel$.SEVR",Double.class));
            _widgetModel.setDynamicsDescriptor(AbstractWidgetModel.PROP_COLOR_FOREGROUND, dynamicsDescriptor);
        }
        if(_bclr!=null){
            _widgetModel.setBackgroundColor(ADLHelper.getRGB(_bclr));
//          /* alarm displayed as background color */
//            DynamicsDescriptor dynamicsDescriptor = new DynamicsDescriptor("org.css.sds.color.default_epics_alarm_background");
//            dynamicsDescriptor.addInputChannel(new ParameterDescriptor("$channel$",Double.class));
//            _widgetModel.setDynamicsDescriptor(AbstractWidgetModel.PROP_COLOR_BACKGROUND, dynamicsDescriptor);
        }
        if(_chan!=null){
            _postfix = ADLHelper.setChan(_widgetModel,_chan);
        }
    }

    /**
     * 
     * @return the postfix (property/Feldname) of the record.
     */
    public final String getPostfix() {
        return _postfix;
    }

    /**
     * 
     * @return the backgoundcolor.
     */
    public final String getBclr() {
        return _bclr;
    }

    /**
     * 
     * @param bclr set the background color.
     */
    public final void setBclr(final String bclr) {
        _bclr = bclr;
    }

    /**
     * 
     * @return get the foreground color.
     */
    public final String getClr() {
        return _clr;
    }

    /**
     * 
     * @param clr set the foreground color.
     */
    public final void setClr(final String clr) {
        _clr = clr;
    }

    /**
     * 
     * @return get the Channel.
     */
    public final String[] getChan() {
        return _chan;
    }

}

