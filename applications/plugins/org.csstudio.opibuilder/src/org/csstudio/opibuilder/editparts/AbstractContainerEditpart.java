/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.editparts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.csstudio.opibuilder.dnd.DropPVtoContainerEditPolicy;
import org.csstudio.opibuilder.dnd.DropPVtoPVWidgetEditPolicy;
import org.csstudio.opibuilder.editpolicies.WidgetContainerEditPolicy;
import org.csstudio.opibuilder.editpolicies.WidgetXYLayoutEditPolicy;
import org.csstudio.opibuilder.model.AbstractContainerModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.util.MacrosInput;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.CompoundSnapToHelper;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToGuides;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.editpolicies.SnapFeedbackPolicy;
import org.eclipse.gef.rulers.RulerProvider;

/**The editpart for {@link AbstractContainerModel}
 * @author Xihui Chen, Sven Wende (class of same name in SDS)
 *
 */
public abstract class AbstractContainerEditpart extends AbstractBaseEditPart {	

	private PropertyChangeListener childrenPropertyChangeListener;
	private PropertyChangeListener selectionPropertyChangeListener;

	@Override
	protected void createEditPolicies() {
		super.createEditPolicies();
		
		installEditPolicy(EditPolicy.CONTAINER_ROLE, new WidgetContainerEditPolicy());
		installEditPolicy(EditPolicy.LAYOUT_ROLE, 
				getExecutionMode() == ExecutionMode.EDIT_MODE ? new WidgetXYLayoutEditPolicy() : null);
		
		//the snap feedback effect
		installEditPolicy("Snap Feedback", new SnapFeedbackPolicy()); //$NON-NLS-1$
		if(getExecutionMode() == ExecutionMode.EDIT_MODE)
			installEditPolicy(DropPVtoPVWidgetEditPolicy.DROP_PV_ROLE, new DropPVtoContainerEditPolicy());
	}
	
	/**Get a child of this container by name.
	 * @param name the name of the child widget
	 * @return the widgetController of the child. null if the child doesn't exist.
	 */
	public AbstractBaseEditPart getChild(String name){
		List<?> children = getChildren();
		for(Object o : children){
			if(o instanceof AbstractBaseEditPart){
				if(((AbstractBaseEditPart)o).getWidgetModel().getName().equals(name))
					return (AbstractBaseEditPart)o;
			}
		}
		return null;
	}
	
	/**Get the widget which is a descendant of the container by name. 
	 * @param name the name of the widget.
	 * @return the widget controller.
	 * @throws Exception If widget with this name doesn't exist
	 */
	public AbstractBaseEditPart getWidget(String name) throws Exception{
		AbstractBaseEditPart widget = searchWidget(name);
		if(widget == null)
			throw new Exception("Widget with name \"" + name + "\" does not exist!");
		else
			return widget;
	}
	
	/**Recursively search the widget
	 * @param name
	 * @return
	 */
	private AbstractBaseEditPart searchWidget(String name){
		AbstractBaseEditPart child = getChild(name);
		if(child != null)
			return child;
		else {			
			for(Object obj : getChildren()){
				if(obj instanceof AbstractContainerEditpart){
					AbstractContainerEditpart containerChild = (AbstractContainerEditpart)obj;
					AbstractBaseEditPart widget = containerChild.searchWidget(name);
					if(widget != null)
						return widget;
				}
			}
		}
		return null;
	}
	
	
	@Override
	public void setModel(Object model) {
		super.setModel(model);
		if(model instanceof AbstractContainerModel){
			//set macro map
			LinkedHashMap<String, String> macrosMap = new LinkedHashMap<String, String>();
			if(getWidgetModel().getMacrosInput().isInclude_parent_macros()){
				macrosMap.putAll(getWidgetModel().getParentMacroMap());
			}
			macrosMap.putAll(getWidgetModel().getMacrosInput().getMacrosMap());		
			getWidgetModel().setMacroMap(macrosMap);
		}
		
	}
	
	@Override
	public void activate() {
		
		
		super.activate();
		if(getExecutionMode() == ExecutionMode.RUN_MODE)
			layout();
		
		childrenPropertyChangeListener = new PropertyChangeListener() {					
					public void propertyChange(PropertyChangeEvent evt) {
						
						if(evt.getOldValue() instanceof Integer){
							addChild(createChild(evt.getNewValue()), ((Integer)evt
									.getOldValue()).intValue());
						}else if (evt.getOldValue() instanceof AbstractWidgetModel){
							EditPart child = (EditPart)getViewer().getEditPartRegistry().get(
									evt.getOldValue());						
							if(child != null)
								removeChild(child);
						}else							
							refreshChildren();						
					}
				};
		getWidgetModel().getChildrenProperty().
			addPropertyChangeListener(childrenPropertyChangeListener);
		
		if(getExecutionMode() == ExecutionMode.EDIT_MODE){
			selectionPropertyChangeListener = new PropertyChangeListener(){
				@SuppressWarnings("unchecked")
				public void propertyChange(PropertyChangeEvent evt) {
					List<AbstractWidgetModel> widgets = (List<AbstractWidgetModel>) evt.getNewValue();
					List<EditPart> widgetEditparts = new ArrayList<EditPart>();
					for(AbstractWidgetModel w : widgets){
						EditPart e = (EditPart)getViewer().getEditPartRegistry().get(w);
						if(e != null)
							widgetEditparts.add(e);
					}
					
					if(!(Boolean)evt.getOldValue()){ //append
						getViewer().deselectAll();
					}
					for(EditPart editpart : widgetEditparts)
						getViewer().appendSelection(editpart);
				}
			};
			
			getWidgetModel().getSelectionProperty().addPropertyChangeListener(
					selectionPropertyChangeListener);
		}
		
		
	}
	
	
	
	@Override
	protected void registerBasePropertyChangeHandlers() {
		super.registerBasePropertyChangeHandlers();
		IWidgetPropertyChangeHandler handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				MacrosInput macrosInput = (MacrosInput)newValue;
				
				LinkedHashMap<String, String> macrosMap = new LinkedHashMap<String, String>();
				if(macrosInput.isInclude_parent_macros()){	
					macrosMap.putAll(getWidgetModel().getParentMacroMap());					
				}		
				macrosMap.putAll(macrosInput.getMacrosMap());
				getWidgetModel().setMacroMap(macrosMap);
				return false;
			}
		};
		setPropertyChangeHandler(AbstractContainerModel.PROP_MACROS, handler);		
		
	}
	
	
	@Override
	public void deactivate() {
		super.deactivate();
		getWidgetModel().getChildrenProperty().
			removeAllPropertyChangeListeners();
		childrenPropertyChangeListener = null;
		getWidgetModel().getSelectionProperty().removeAllPropertyChangeListeners();
		selectionPropertyChangeListener = null;
	}
	
	@Override
	protected List<AbstractWidgetModel> getModelChildren() {
		return ((AbstractContainerModel)getModel()).getChildren();
	}

	@Override
	public AbstractContainerModel getWidgetModel() {
		return (AbstractContainerModel)getModel();
	}
	
	public AbstractLayoutEditpart getLayoutWidget(){
		for(Object child : getChildren()){
			if(child instanceof AbstractLayoutEditpart){
				return (AbstractLayoutEditpart)child;
			}
		}
		return null;
	}
	
	public void layout(){
		AbstractLayoutEditpart layoutter = getLayoutWidget();
		if(layoutter != null && layoutter.getWidgetModel().isEnabled()){
			List<AbstractWidgetModel> modelChildren = new ArrayList<AbstractWidgetModel>();
			modelChildren.addAll(getModelChildren());
			modelChildren.remove(layoutter.getWidgetModel());
			layoutter.layout(modelChildren, getFigure().getClientArea());
		}
	}
	

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		//make snap to G work
		if (adapter == SnapToHelper.class) {
			List<SnapToHelper> snapStrategies = new ArrayList<SnapToHelper>();
			Boolean val = (Boolean)getViewer().getProperty(RulerProvider.PROPERTY_RULER_VISIBILITY);
			if (val != null && val.booleanValue())
				snapStrategies.add(new SnapToGuides(this));
			val = (Boolean)getViewer().getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED);
			if (val != null && val.booleanValue())
				snapStrategies.add(new SnapToGeometry(this));
			val = (Boolean)getViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED);
			if (val != null && val.booleanValue())
				snapStrategies.add(new SnapToGrid(this));
			
			if (snapStrategies.size() == 0)
				return null;
			if (snapStrategies.size() == 1)
				return snapStrategies.get(0);

			SnapToHelper ss[] = new SnapToHelper[snapStrategies.size()];
			for (int i = 0; i < snapStrategies.size(); i++)
				ss[i] = snapStrategies.get(i);
			return new CompoundSnapToHelper(ss);
		}
		return super.getAdapter(adapter);
	}

}
