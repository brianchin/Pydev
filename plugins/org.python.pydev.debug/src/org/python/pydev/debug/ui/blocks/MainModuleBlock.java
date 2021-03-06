/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.dialogs.PythonModulePickerDialog;

/**
 * A control for selecting a python module.
 */
public class MainModuleBlock extends AbstractLaunchConfigurationTab {

    private Text fMainModuleText;
    private Button fMainModuleBrowseButton;
    private String fProjectName;
    private ModifyListener fProjectModifyListener;

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        setControl(group);
        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        group.setLayout(topLayout);    
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        group.setFont(font);
        group.setText("Main Module"); 
        
        fMainModuleText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fMainModuleText.setLayoutData(gd);
        fMainModuleText.setFont(font);
        fMainModuleText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });    

        final Composite lParent = parent;
        fMainModuleBrowseButton = createPushButton(group, "Browse...", null);
        fMainModuleBrowseButton.setText("Browse");
        
        // On button click, this displays the python module picker dialog.
        fMainModuleBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IFile currentFile = getMainModuleFile();
                IResource resource = workspace.getRoot().findMember(fProjectName);

                if (resource instanceof IProject) {
                    IProject project = (IProject) resource;
                    PythonModulePickerDialog dialog = new PythonModulePickerDialog(
                            lParent.getShell(), 
                            "Main Module",
                            "Choose Python module which starts execution",
                            project);
                    
                    // Fixed request 1407469: main module browse button forgets path                    
                    dialog.setInitialSelection(currentFile);

                    int result = dialog.open();
                    if (result == PythonModulePickerDialog.OK) {
                        Object results[] = dialog.getResult();
                        if (   (results != null) 
                            && (results.length > 0)
                            && (results[0] instanceof IFile)) {
                            IFile file = (IFile) results[0];
                            IPath path = file.getFullPath();
                            String containerName = path.makeRelative().toString();
                            fMainModuleText.setText("${workspace_loc:" + containerName + "}");
                        }
                    }
                }
            }
        });
        
        // Create a ModifyListener, used to listen for project modifications in the ProjectBlock. 
        // This assumes that the Project is in a Text control...
        fProjectModifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                Widget widget = e.widget;
                if (widget instanceof Text) {
                    Text text = (Text) widget;
                    fProjectName = text.getText();
                    IWorkspace workspace = ResourcesPlugin.getWorkspace();
                    IResource resource = workspace.getRoot().findMember(fProjectName);

                    boolean enabled = false;
                    if (   (resource != null)
                        && (resource instanceof IProject)) {
                        IProject project = (IProject) resource;
                        PythonNature nature = PythonNature.getPythonNature(project);
                        enabled = (nature != null);
                    }
                    
                    fMainModuleBrowseButton.setEnabled(enabled);
                }
            }
        };        
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Main module";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration configuration) {
        
        // Initialize the location field
        String location = "";
        try {            
            location = configuration.getAttribute(Constants.ATTR_LOCATION, "");
        } catch(CoreException e) { }
        fMainModuleText.setText(location);
        
        // Obtain a copy of the project name (not displayed)
        String projectName = "";
        try {            
            projectName = configuration.getAttribute(Constants.ATTR_PROJECT, "");
        } catch(CoreException e) { }
        fProjectName = projectName;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        String value = fMainModuleText.getText().trim();
        setAttribute(configuration, Constants.ATTR_LOCATION, value);
        configuration.setMappedResources(new IResource[]{getMainModuleFile()});
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        //no defaults to set
    }

    /**
     * Obtains an IFile that targets the current main module.
     * 
     * This is used for initializing the module selection dialog.
     * 
     * @return The main module file. 
     */
    private IFile getMainModuleFile() {
        String path = fMainModuleText.getText();
        IFile file = null;
        if (path.length() > 0) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            
            StringSubstitution stringSubstitution = getStringSubstitution(root);
            try {
                path = stringSubstitution.performStringSubstitution(path, false);
                IFile[] files = root.findFilesForLocationURI(new File(path).toURI());
                if (files.length > 0) {
                    file = files[0];
                }
            } 
            catch (CoreException e) {
                Log.log(e);
            }
                
        }
        return file;
    }

    /**
     * @param root the workspace root.
     * @return an object capable on making string substitutions based on variables in the project and in the workspace.
     */
    public StringSubstitution getStringSubstitution(IWorkspaceRoot root){
        IResource resource = root.findMember(fProjectName);
        IPythonNature nature = null;
        if (resource instanceof IProject) {
            nature = PythonNature.getPythonNature(resource);
        }
        
        StringSubstitution stringSubstitution = new StringSubstitution(nature);
        return stringSubstitution;
    }

    /**
     * Sets attributes in the working copy
     * 
     * @param configuration The configuration to set the attribute in
     * @param name Name of the attribute to set
     * @param value Value to set 
     */
    private void setAttribute(ILaunchConfigurationWorkingCopy configuration, String name, String value) {
        if (value == null || value.length() == 0){
            configuration.setAttribute(name, (String) null);
        }else{
            configuration.setAttribute(name, value);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
     @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        boolean result = super.isValid(launchConfig);
        
        if (result) {
            setMessage(null);
            setErrorMessage(null);
            
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            StringSubstitution stringSubstitution = getStringSubstitution(root);

            String location = fMainModuleText.getText();
            try {
                
                String identifier = launchConfig.getType().getIdentifier();
                if(
                    identifier.equals(Constants.ID_PYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE) || 
                    identifier.equals(Constants.ID_JYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE) ||
                    identifier.equals(Constants.ID_IRONPYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE) || 
                    identifier.equals(Constants.ID_PYTHON_COVERAGE_LAUNCH_CONFIGURATION_TYPE)){
                    
                    //may have  multiple files selected for the run for unitest and code-coverage
                    for(String loc:StringUtils.splitAndRemoveEmptyTrimmed(location, '|')){
                        String expandedLocation = stringSubstitution.performStringSubstitution(loc);
                        File file = new File(expandedLocation);
                        if(!file.exists()){
                            setErrorMessage(StringUtils.format("The file \"%s\" does not exist.", file));
                            result = false;
                            break;
                        }
                        
                    }
                }else{
                    String expandedLocation = stringSubstitution.performStringSubstitution(location);
                    File file = new File(expandedLocation);
                    if(!file.exists()){
                        setErrorMessage(StringUtils.format("The file \"%s\" does not exist.", file));
                        result = false;
                        
                    }else if(!file.isFile()) {
                        setErrorMessage(StringUtils.format("The file \"%s\" does not actually map to a file.", file));
                        result = false;
                    }
                }
                
            } catch (CoreException e) {
                setErrorMessage("Unable to resolve location");
                result = false;
            }
        }
        return result;
    }
     

    /**
     * Obtain a listener, used to detect changes of the currently selected project
     * This updates the browse button, and allos the appropriate selection of the main module.
     *  
     * @return a ModifyListener that updates the block controls.
     */
    public ModifyListener getProjectModifyListener() {
        return fProjectModifyListener;
    }
}
