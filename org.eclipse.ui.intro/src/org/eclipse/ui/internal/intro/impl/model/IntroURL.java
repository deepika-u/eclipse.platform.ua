/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.intro.impl.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.RectangleAnimation;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.parts.StandbyPart;
import org.eclipse.ui.internal.intro.impl.presentations.BrowserIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.presentations.IntroLaunchBar;
import org.eclipse.ui.internal.intro.impl.util.DialogUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.Util;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.CustomizableIntroPart;
import org.eclipse.ui.intro.config.IIntroAction;
import org.eclipse.ui.intro.config.IIntroURL;
import org.eclipse.ui.intro.config.IntroURLFactory;

/**
 * An intro url. An intro URL is a valid http url, with org.eclipse.ui.intro as
 * a host. This class holds all logic to execute Intro URL commands, ie: an
 * Intro URL knows how to execute itself.
 */
public class IntroURL implements IIntroURL {


    /**
     * Intro URL constants.
     */
    public static final String INTRO_PROTOCOL = "http"; //$NON-NLS-1$
    public static final String INTRO_HOST_ID = "org.eclipse.ui.intro"; //$NON-NLS-1$

    /**
     * Constants that represent Intro URL actions.
     */
    public static final String SET_STANDBY_MODE = "setStandbyMode"; //$NON-NLS-1$
    public static final String SHOW_STANDBY = "showStandby"; //$NON-NLS-1$
    public static final String CLOSE = "close"; //$NON-NLS-1$
    public static final String SHOW_HELP_TOPIC = "showHelpTopic"; //$NON-NLS-1$
    public static final String SHOW_HELP = "showHelp"; //$NON-NLS-1$
    public static final String OPEN_BROWSER = "openBrowser"; //$NON-NLS-1$
    public static final String OPEN_URL = "openURL"; //$NON-NLS-1$
    public static final String RUN_ACTION = "runAction"; //$NON-NLS-1$
    public static final String SHOW_PAGE = "showPage"; //$NON-NLS-1$
    public static final String SHOW_MESSAGE = "showMessage"; //$NON-NLS-1$
    public static final String NAVIGATE = "navigate"; //$NON-NLS-1$
    public static final String SWITCH_TO_LAUNCH_BAR = "switchToLaunchBar"; //$NON-NLS-1$

    /**
     * Constants that represent valid action keys.
     */
    public static final String KEY_ID = "id"; //$NON-NLS-1$
    public static final String KEY_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
    public static final String KEY_CLASS = "class"; //$NON-NLS-1$
    public static final String KEY_STANDBY = "standby"; //$NON-NLS-1$
    public static final String KEY_PART_ID = "partId"; //$NON-NLS-1$
    public static final String KEY_INPUT = "input"; //$NON-NLS-1$
    public static final String KEY_MESSAGE = "message"; //$NON-NLS-1$
    public static final String KEY_URL = "url"; //$NON-NLS-1$
    public static final String KEY_DIRECTION = "direction"; //$NON-NLS-1$
    public static final String KEY_EMBED = "embed"; //$NON-NLS-1$


    public static final String VALUE_BACKWARD = "backward"; //$NON-NLS-1$
    public static final String VALUE_FORWARD = "forward"; //$NON-NLS-1$
    public static final String VALUE_HOME = "home"; //$NON-NLS-1$
    public static final String VALUE_TRUE = "true"; //$NON-NLS-1$



    private String action = null;
    private Properties parameters = null;

    /**
     * Prevent creation. Must be created through an IntroURLParser. This
     * constructor assumed we have a valid intro url.
     * 
     * @param url
     */
    IntroURL(String action, Properties parameters) {
        this.action = action;
        this.parameters = parameters;
    }

    /**
     * Executes whatever valid Intro action is embedded in this Intro URL.
     * 
     */
    public boolean execute() {
        final boolean[] result = new boolean[1];
        Display display = Display.getCurrent();
        BusyIndicator.showWhile(display, new Runnable() {

            public void run() {
                result[0] = doExecute();
            }
        });
        return result[0];
    }

    protected boolean doExecute() {

        // check for all supported Intro actions first.
        if (action.equals(CLOSE))
            return closeIntro();

        else if (action.equals(SET_STANDBY_MODE))
            // Sets the state of the intro part. Does not care about passing
            // input to the part.
            return setStandbyState(getParameter(KEY_STANDBY));

        else if (action.equals(SHOW_STANDBY))
            return handleStandbyState(getParameter(KEY_PART_ID),
                getParameter(KEY_INPUT));

        else if (action.equals(SHOW_HELP))
            // display the full Help System.
            return showHelp();

        else if (action.equals(SHOW_HELP_TOPIC))
            // display a Help System Topic. It can be displayed in the Help
            // system window, or embedded as an intro page.
            // return showHelpTopic(getParameter(KEY_ID));
            return showHelpTopic(getParameter(KEY_ID), getParameter(KEY_EMBED));

        else if (action.equals(OPEN_BROWSER))
            // display url in external browser
            return openBrowser(getParameter(KEY_URL),
                getParameter(KEY_PLUGIN_ID));

        if (action.equals(OPEN_URL))
            // display url embedded in intro browser.
            return openURL(getParameter(KEY_URL), getParameter(KEY_PLUGIN_ID));

        else if (action.equals(RUN_ACTION))
            // run an Intro action. Get the pluginId and the class keys. Pass
            // the parameters and the standby state.
            return runAction(getParameter(KEY_PLUGIN_ID),
                getParameter(KEY_CLASS), parameters, getParameter(KEY_STANDBY));

        else if (action.equals(SHOW_PAGE))
            // display an Intro Page.
            return showPage(getParameter(KEY_ID), getParameter(KEY_STANDBY));

        else if (action.equals(SHOW_MESSAGE))
            return showMessage(getParameter(KEY_MESSAGE));

        else if (action.equals(NAVIGATE))
            return navigate(getParameter(KEY_DIRECTION));

        else if (action.equals(SWITCH_TO_LAUNCH_BAR))
            return switchToLaunchBar();
        else
            return handleCustomAction();
    }


    private boolean closeIntro() {
        // Relies on Workbench.
        return IntroPlugin.closeIntro();
    }

    /**
     * Sets the into part to standby, and shows the passed standby part, with
     * the given input. Forces the Intro view to open, if not yet created.
     * 
     * @param partId
     * @param input
     */
    private boolean handleStandbyState(String partId, String input) {
        // set intro to standby mode. we know we have a customizable part.
        CustomizableIntroPart introPart = (CustomizableIntroPart) IntroPlugin
            .getIntro();
        if (introPart == null)
            introPart = (CustomizableIntroPart) IntroPlugin.showIntro(true);
        // store the flag to indicate that standbypart is needed.
        introPart.getControl().setData(IIntroConstants.SHOW_STANDBY_PART,
            VALUE_TRUE); //$NON-NLS-1$
        IntroPlugin.setIntroStandby(true);
        StandbyPart standbyPart = (StandbyPart) introPart
            .getAdapter(StandbyPart.class);

        boolean success = standbyPart.showContentPart(partId, input);
        if (success)
            return true;

        // we do not have a valid partId or we failed to instantiate part or
        // create the part content, empty part will be shown. Signal failure.
        return false;
    }

    /**
     * Set the Workbench Intro Part state. Forces the Intro view to open, if not
     * yet created.
     * 
     * @param state
     */
    private boolean setStandbyState(String state) {
        if (state == null)
            return false;
        boolean standby = state.equals(VALUE_TRUE) ? true : false; //$NON-NLS-1$
        IIntroPart introPart = IntroPlugin.showIntro(standby);
        if (introPart == null)
            return false;
        return true;
    }


    /**
     * Run an action
     */
    private boolean runAction(String pluginId, String className,
            Properties parameters, String standbyState) {

        Object actionObject = ModelLoaderUtil.createClassInstance(pluginId,
            className);
        try {
            if (actionObject instanceof IIntroAction) {
                IIntroAction introAction = (IIntroAction) actionObject;
                IIntroSite site = IntroPlugin.getDefault().getIntroModelRoot()
                    .getPresentation().getIntroPart().getIntroSite();
                introAction.run(site, parameters);
            } else if (actionObject instanceof IAction) {
                IAction action = (IAction) actionObject;
                action.run();

            } else if (actionObject instanceof IActionDelegate) {
                final IActionDelegate delegate = (IActionDelegate) actionObject;
                if (delegate instanceof IWorkbenchWindowActionDelegate)
                    ((IWorkbenchWindowActionDelegate) delegate).init(PlatformUI
                        .getWorkbench().getActiveWorkbenchWindow());
                Action proxy = new Action(this.action) {

                    public void run() {
                        delegate.run(this);
                    }
                };
                proxy.run();
            } else
                // we could not create the class.
                return false;
            // ran action successfully. Now set intro intro standby if needed.
            if (standbyState == null)
                return true;
            return setStandbyState(standbyState);
        } catch (Exception e) {
            Log.error("Could not run action: " + className, e); //$NON-NLS-1$
            return false;
        }
    }


    /**
     * Open a help topic. If embed="true", open the help href as an intro page.
     * If false, open the href in the Help system window. In the case of SWT
     * presentation, embedd flag is ignored and the topic is opened in the Help
     * system window.
     */
    private boolean showHelpTopic(String href, String embed) {
        if (href == null)
            return false;

        boolean isEmbedded = (embed != null && embed.equals(VALUE_TRUE)) ? true
                : false;
        IntroModelRoot model = IntroPlugin.getDefault().getIntroModelRoot();
        String presentationStyle = model.getPresentation()
            .getImplementationKind();

        if (isEmbedded
                && presentationStyle
                    .equals(IntroPartPresentation.BROWSER_IMPL_KIND)) {
            // we want embedded and we have HTML presentation, show href
            // embedded.
            BrowserIntroPartImplementation impl = (BrowserIntroPartImplementation) model
                .getPresentation().getIntroParttImplementation();
            // INTRO: maybe add support for navigation
            href = PlatformUI.getWorkbench().getHelpSystem()
                .resolve(href, true).toExternalForm();
            impl.getBrowser().setUrl(href);
            return true;
        }

        // show href in Help window. SWT presentation is handled here.
        // WorkbenchHelp takes care of error handling.
        PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
        return true;

    }



    /**
     * Open the help system.
     */
    private boolean showHelp() {
        PlatformUI.getWorkbench().getHelpSystem().displayHelp();
        return true;
    }

    /**
     * Launch external browser
     */
    private boolean openBrowser(String url, String pluginId) {
        // no need to decode url because we will create another url from this
        // url anyway. Resolve the url just in case we are trying to load a
        // plugin relative file.
        url = ModelUtil.resolveURL(url, pluginId);
        return Util.openBrowser(url);
    }


    /**
     * Show a URL in an intro page. This is the embedded version of the intro
     * action openBrowser(). It is useful when trying to show an html file
     * relative to another plugin. When the presentation is UI forms
     * presentation, this call behaves exactly as the openBrowser intro action.
     */

    private boolean openURL(String url, String pluginId) {
        IntroModelRoot model = IntroPlugin.getDefault().getIntroModelRoot();
        String presentationStyle = model.getPresentation()
            .getImplementationKind();

        if (presentationStyle.equals(IntroPartPresentation.BROWSER_IMPL_KIND)) {
            // HTML presentation
            url = ModelUtil.resolveURL(url, pluginId);
            BrowserIntroPartImplementation impl = (BrowserIntroPartImplementation) IntroPlugin
                .getDefault().getIntroModelRoot().getPresentation()
                .getIntroParttImplementation();
            Browser browser = impl.getBrowser();
            return browser.setUrl(url);
        }
        {
            // SWT presentation.
            return openBrowser(url, pluginId);
        }
    }


    private boolean showMessage(String message) {
        if (message == null)
            return false;
        try {
            message = URLDecoder.decode(message, "UTF-8"); //$NON-NLS-1$
            DialogUtil.displayInfoMessage(null, message);
            return true;
        } catch (UnsupportedEncodingException e) {
            DialogUtil.displayInfoMessage(null, "IntroURL.failedToDecode", //$NON-NLS-1$
                new Object[] { message });
            return false;
        }
    }

    /**
     * Display an Intro Page.
     * <p>
     * INTRO: revisit picking first page.
     */
    private boolean showPage(String pageId, String standbyState) {
        // set the current page id in the model. This will triger appropriate
        // listener event to the UI. If setting the page in the model fails (ie:
        // the page was not found in the current model, look for it in loaded
        // models. return false if failed.
        // avoid flicker.
        CustomizableIntroPart currentIntroPart = (CustomizableIntroPart) IntroPlugin
            .getIntro();
        currentIntroPart.getControl().setRedraw(false);

        IntroModelRoot modelRoot = IntroPlugin.getDefault().getIntroModelRoot();
        boolean success = modelRoot.setCurrentPageId(pageId);
        if (!success)
            success = includePageToShow(modelRoot, pageId);

        // we turned drawing off. Turn it on again.
        currentIntroPart.getControl().setRedraw(true);

        if (success) {
            // found page
            modelRoot.getPresentation().updateHistory(pageId);
            // ran action successfully. Now set intro intro standby if needed.
            if (standbyState == null)
                return true;
            return setStandbyState(standbyState);
        }
        // could not find referenced page.
        return false;
    }

    /**
     * Finds the target page and includes it in passed model.
     * 
     * @param pageId
     * @return
     */
    private boolean includePageToShow(IntroModelRoot model, String pageId) {
        AbstractIntroPage page = findPageToShow(pageId);
        if (page == null) {
            Log.error("Failed to clone Intro page.", null); //$NON-NLS-1$
            return false;
        }
        // now clone the target page because original model should be kept
        // intact. Resolve target page first to resolve its includes
        // properly. Insert presentation shared style at the top of the shared
        // styles list because once reparented, the shared style is lost.
        // Finally, add clone page to current model.
        page.getChildren();
        // current kind.
        String currentPresentationKind = model.getPresentation()
            .getImplementationKind();
        // load shared style corresponding to same presentation kind from target
        // model.
        IntroPartPresentation targetPresentation = ((IntroModelRoot) page
            .getParent()).getPresentation();
        String targetSharedStyle = targetPresentation
            .getSharedStyle(currentPresentationKind);
        // clone.
        AbstractIntroPage clonedPage = null;
        try {
            clonedPage = (AbstractIntroPage) page.clone();
        } catch (CloneNotSupportedException ex) {
            // should never be here.
            Log.error("Failed to clone Intro model node.", ex); //$NON-NLS-1$
            return false;
        }
        // reparent cloned target to current model.
        clonedPage.setParent(model);
        // REVISIT: SWT presentation does not support multiple shared
        // styles.
        if (targetSharedStyle != null)
            // add target model shared style.
            clonedPage.insertStyle(targetSharedStyle, 0);
        model.children.add(clonedPage);
        return model.setCurrentPageId(clonedPage.getId());
    }


    /**
     * Searches all loaded models for the first page with the given id.
     * 
     * @param pageId
     * @return
     */
    private AbstractIntroPage findPageToShow(String pageId) {
        // get all cached models.
        Hashtable models = ExtensionPointManager.getInst().getIntroModels();
        Enumeration values = models.elements();
        while (values.hasMoreElements()) {
            IntroModelRoot model = (IntroModelRoot) values.nextElement();
            AbstractIntroPage page = (AbstractIntroPage) model.findChild(
                pageId, AbstractIntroElement.ABSTRACT_PAGE);
            if (page != null)
                return page;
        }
        // could not find page in any model.
        return null;
    }

    /**
     * Navigate foward in the presentation, whichever one it is.
     * 
     * @return
     */
    private boolean navigate(String direction) {
        // set intro to standby mode. we know we have a customizable part.
        CustomizableIntroPart introPart = (CustomizableIntroPart) IntroPlugin
            .getIntro();
        if (introPart == null)
            // intro is closed. Do nothing.
            return false;

        IntroPartPresentation presentation = (IntroPartPresentation) introPart
            .getAdapter(IntroPartPresentation.class);

        if (direction.equalsIgnoreCase(VALUE_BACKWARD))
            return presentation.navigateBackward();
        else if (direction.equalsIgnoreCase(VALUE_FORWARD))
            return presentation.navigateForward();
        else if (direction.equalsIgnoreCase(VALUE_HOME))
            return presentation.navigateHome();
        return false;
    }


    /**
     * @return Returns the action imbedded in this URL.
     */
    public String getAction() {
        return action;
    }

    /**
     * Return a parameter defined in the Intro URL. Returns null if the
     * parameter is not defined.
     * 
     * @param parameterId
     * @return
     */
    public String getParameter(String parameterId) {
        return parameters.getProperty(parameterId);
    }

    private boolean handleCustomAction() {
        IntroURLAction command = ExtensionPointManager.getInst()
            .getSharedConfigExtensionsManager().getCommand(action);
        if (command == null) {
            DialogUtil.displayInfoMessage(null, "IntroURL.badCommand", //$NON-NLS-1$
                new Object[] { action });
            return false;
        }

        // custom command. execute it.
        StringBuffer url = new StringBuffer();
        url.append("http://org.eclipse.ui.intro/"); //$NON-NLS-1$
        url.append(command.getReplaceValue().trim());
        if (command.getReplaceValue().indexOf("?") == -1) //$NON-NLS-1$
            // command does not have parameters.
            url.append("?"); //$NON-NLS-1$
        else
            // command already has parameters.
            url.append("&"); //$NON-NLS-1$
        url.append(retrieveInitialQuery());
        IIntroURL introURL = IntroURLFactory.createIntroURL(url.toString());
        if (introURL != null)
            return introURL.execute();
        return false;
    }


    /**
     * Recreate the initial query passed to this URL.
     * 
     * @return
     */
    private String retrieveInitialQuery() {
        StringBuffer query = new StringBuffer();
        Enumeration keys = parameters.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            query.append(key);
            query.append("="); //$NON-NLS-1$
            query.append(parameters.get(key));
            if (keys.hasMoreElements())
                query.append("&"); //$NON-NLS-1$
        }
        return query.toString();
    }

    private boolean switchToLaunchBar() {
        IIntroPart intro = PlatformUI.getWorkbench().getIntroManager()
            .getIntro();
        if (intro == null)
            return false;

        CustomizableIntroPart cpart = (CustomizableIntroPart) intro;
        IntroModelRoot modelRoot = IntroPlugin.getDefault().getIntroModelRoot();
        String pageId = modelRoot.getCurrentPageId();
        Rectangle bounds = cpart.getControl().getBounds();
        Rectangle startBounds = Geometry.toDisplay(cpart.getControl()
            .getParent(), bounds);
        closeIntro();

        IWorkbenchWindow window = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow();
        LaunchBarElement launchBarElement = modelRoot.getPresentation()
            .getLaunchBar();
        IntroLaunchBar launchBar = new IntroLaunchBar(launchBarElement
            .getOrientation(), pageId, launchBarElement);
        launchBar.createInActiveWindow();
        Rectangle endBounds = Geometry.toDisplay(launchBar.getControl()
            .getParent(), launchBar.getControl().getBounds());

        RectangleAnimation animation = new RectangleAnimation(
            window.getShell(), startBounds, endBounds);
        animation.schedule();
        return true;
    }
}
