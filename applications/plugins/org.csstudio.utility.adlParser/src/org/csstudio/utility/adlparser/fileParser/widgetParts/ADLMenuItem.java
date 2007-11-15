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

import java.util.HashMap;
import java.util.Map;

import org.csstudio.sds.model.AbstractWidgetModel;
import org.csstudio.sds.model.properties.ActionData;
import org.csstudio.sds.model.properties.ActionType;
import org.csstudio.sds.model.properties.actions.CommitValueWidgetAction;
import org.csstudio.sds.model.properties.actions.OpenDisplayWidgetAction;
import org.csstudio.utility.adlconverter.utility.ADLWidget;
import org.csstudio.utility.adlconverter.utility.WrongADLFormatException;
import org.eclipse.core.runtime.Path;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 21.09.2007
 */
public class ADLMenuItem extends WidgetPart{

    /**
     * The displayed text and description of the Action.
     */
    private String _label;
    /**
     * The type of Action.
     */
    private String _type;
    /**
     * The relative path to faceplate, script, ...
     */
    private String _command;
    /**
     * The arguments. Normal a Channel names.
     */
    private String _args;
    /**
     * The root path for Widget.
     */
    private String _path; 

    /**
     * The default constructor.
     * 
     * @param menuItem An ADLWidget that correspond a ADL Menu Item. 
     * @param parentWidgetModel The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    public ADLMenuItem(final ADLWidget menuItem, final AbstractWidgetModel parentWidgetModel) throws WrongADLFormatException {
        super(menuItem, parentWidgetModel);
    }
    
    /**
     * {@inheritDoc}
     */
    final void init(){
        _path = "/SDS"; //"C:/Helge_Projekte/CSS 3.3 Workspace/SDS/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void parseWidgetPart(final ADLWidget menuItem) throws WrongADLFormatException {
        assert !menuItem.isType("menuItem") :  "This "+menuItem.getType()+" is not a ADL Menu Item";

        for (String parameter : menuItem.getBody()) {
            if(parameter.trim().startsWith("//")){
                continue;
            }
            String[] row = parameter.split("=");
//            if(row.length!=2){
//                throw new Exception("This "+parameter+" is a wrong ADL Menu Item");
//            }
            if(row[0].trim().toLowerCase().equals("label")){
                _label=row[1].trim();
            }else if(row[0].trim().toLowerCase().equals("type")){
                _type=row[1].trim();
            }else if(row[0].trim().toLowerCase().equals("command")){
                _command=row[1].trim();
            }else if(row[0].trim().toLowerCase().equals("args")){
                _args=parameter.substring(parameter.indexOf("=")+1).replaceAll("\"","").trim();
            }else {
                throw new WrongADLFormatException("This "+parameter+" is a wrong ADL Menu Item");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void generateElements(){
        ActionData actionData = new ActionData();
        if(_type.equals("\"New Display\"")){
            // new Open Shell Action
            OpenDisplayWidgetAction action = (OpenDisplayWidgetAction) ActionType.OPEN_SHELL
            .getActionFactory().createWidgetAction();
            
            //Set the Resource
            action.getProperty(OpenDisplayWidgetAction.PROP_RESOURCE)
            .setPropertyValue(new Path(_path+_command.replaceAll("\"", "").replace(".adl", ".css-sds")));//TODO: set the correct Path
            actionData.addAction(action);

//            _actionAttribut.addContent(propertyAttribut);
            
            String[] maps = _args.split(","); // TODO: es werde teilweise mehrere Argumente �ber geben.  Momenatan wird aber nur das erste ausgewertet.
            String[] map = maps[0].split("=");
            if(map.length==2){
                Map<String, String> test = new HashMap<String,String>();
                test.put("param", map[1]);
                // Set the aliases
                action.getProperty(OpenDisplayWidgetAction.PROP_ALIASES)
                .setPropertyValue(test);
            } else if(map.length==1){
                //TODO: was ist das f�r ein parameter? dateiendung = stc.
            } else{
                System.out.println("Ung�ltige l�nge");
            }
            action.getProperty(OpenDisplayWidgetAction.PROP_DESCRIPTION)
            .setPropertyValue(_label.replaceAll("\"",""));

        }else if(_type.equals("\"System script\"")){
            CommitValueWidgetAction action = (CommitValueWidgetAction) ActionType.COMMIT_VALUE
            .getActionFactory().createWidgetAction();

            action.getProperty(CommitValueWidgetAction.PROP_VALUE)
            .setPropertyValue(new Path(_label.replaceAll("\"","")));
            actionData.addAction(action);

            action.getProperty(CommitValueWidgetAction.PROP_DESCRIPTION)
            .setPropertyValue(new Path(_label.replaceAll("\"","")));
            actionData.addAction(action);

        }
        
        _widgetModel.setPropertyValue(AbstractWidgetModel.PROP_ACTIONDATA, actionData);
    }
}
