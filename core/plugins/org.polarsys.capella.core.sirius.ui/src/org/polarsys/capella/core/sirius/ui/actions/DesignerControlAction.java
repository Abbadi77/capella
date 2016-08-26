/*******************************************************************************
 * Copyright (c) 2006, 2016 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thales - initial API and implementation
 *******************************************************************************/
package org.polarsys.capella.core.sirius.ui.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.ui.dialogs.ResourceDialog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.window.Window;
import org.eclipse.sirius.business.api.control.SiriusControlCommand;
import org.eclipse.sirius.business.api.control.SiriusUncontrolCommand;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.common.tools.api.resource.ResourceSetSync;
import org.eclipse.sirius.common.tools.api.resource.ResourceSetSync.ResourceStatus;
import org.eclipse.sirius.ui.tools.api.control.SiriusControlHandler;
import org.eclipse.sirius.ui.tools.api.control.SiriusUncontrolHandler;
import org.eclipse.sirius.viewpoint.DAnalysis;
import org.eclipse.sirius.viewpoint.DRepresentation;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.eclipse.sirius.viewpoint.ViewpointPackage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.polarsys.capella.common.ef.command.AbstractNonDirtyingCommand;
import org.polarsys.capella.common.ef.command.AbstractReadOnlyCommand;
import org.polarsys.capella.common.ef.command.ICommand;
import org.polarsys.capella.common.helpers.EcoreUtil2;
import org.polarsys.capella.common.helpers.TransactionHelper;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;
import org.polarsys.capella.common.tools.report.EmbeddedMessage;
import org.polarsys.capella.common.tools.report.config.registry.ReportManagerRegistry;
import org.polarsys.capella.common.tools.report.util.IReportManagerDefaultComponents;
import org.polarsys.capella.common.ui.services.helper.EObjectLabelProviderHelper;
import org.polarsys.capella.core.commands.preferences.service.AbstractPreferencesInitializer;
import org.polarsys.capella.core.model.handler.AbortedTransactionException;
import org.polarsys.capella.core.model.handler.helpers.RepresentationHelper;
import org.polarsys.capella.core.model.handler.pre.commit.listener.FileModificationPreCommitListener;
import org.polarsys.capella.core.platform.sirius.ui.preferences.ICapellaPreferences;
import org.polarsys.capella.core.sirius.ui.helper.SessionHelper;
import org.polarsys.capella.core.sirius.ui.internal.UncontrolMessageDialog;

import com.google.common.collect.Sets;

/**
 * A specific control action handling representations.
 */
public class DesignerControlAction extends ControlAction {
  public class CapellaSiriusControlCommand extends SiriusControlCommand {
    /**
     * Constructor.
     * @param semanticRoot_p
     * @param semanticDest_p
     * @param representations_p
     * @param representationsDest_p
     */
    public CapellaSiriusControlCommand(EObject semanticRoot, URI semanticDest, Set<DRepresentationDescriptor> repDescriptors, URI representationsDest) {
      super(semanticRoot, semanticDest, repDescriptors, representationsDest, new NullProgressMonitor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EObject getRootContainer(EObject eObject) {
      return super.getRootContainer(eObject);
    }
  }

  /**
   * Fix Sirius bugs.
   */
  public class CapellaSiriusUncontrolCommand extends SiriusUncontrolCommand {
    /**
     * Same data as eObject but I can access it without discouraged accesses.
     */
    private EObject _semanticRoot;

    /**
     * Constructor.
     * @param semanticRoot_p
     * @param uncontrolRepresentations_p
     */
    public CapellaSiriusUncontrolCommand(EObject semanticRoot, boolean uncontrolRepresentations) {
      super(semanticRoot, uncontrolRepresentations, new NullProgressMonitor());
      _semanticRoot = semanticRoot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doExecute() {
      super.doExecute();
      // Force to refresh Capella cross referencer data.
      // When specified semantic root object is detached from its own resource, no event is sent to tell it is in its parent resource.
      // Only its resource attribute is set to null with the same container (the container did not changed).
      // But when removing from its own resource has cleaned up cross referencer maps, hence fake an event to populate again cross referencers maps.
      SemanticEditingDomain editingDomain = (SemanticEditingDomain) TransactionHelper.getEditingDomain(_semanticRoot);
      // Get the containing feature.
      EStructuralFeature containingFeature = _semanticRoot.eContainmentFeature();
      int eventType = containingFeature.isMany() ? Notification.ADD : Notification.SET;
      // Create a faked notification.
      ENotificationImpl notification = new ENotificationImpl((InternalEObject) _semanticRoot.eContainer(), eventType, containingFeature, null, _semanticRoot);
      // Broadcast the notif to Capella cross referencers.
      // FIXME It might be interesting to broadcast this faked notif to the whole eAdapters() ?
      editingDomain.getCrossReferencer().notifyChanged(notification);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getAirdResourceWithAnalysisOn(EObject object) {
      return super.getAirdResourceWithAnalysisOn(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<DAnalysis> getAnalyses(Resource aird) {
      return super.getAnalyses(aird);
    }

    /**
     * Overridden to get the right container i.e the first one that is serialized in its own resource. {@inheritDoc}
     */
    @Override
    protected EObject getRootContainer(EObject eObject) {
      return super.getRootContainer(eObject);
    }
  }

  public class CapellaSiriusControlHandler extends SiriusControlHandler {

    protected Shell _shell;

    /**
     * @param shell_p
     */
    public CapellaSiriusControlHandler(Shell shell) {
      _shell = shell;
    }

    /**
     * @see org.eclipse.sirius.ui.tools.api.control.SiriusControlHandler#createControlResourceDialog(org.eclipse.swt.widgets.Shell, java.lang.String)
     */
    @SuppressWarnings("synthetic-access")
    @Override
    protected ResourceDialog createControlResourceDialog(Shell shell, String defaultURI) {
      org.polarsys.capella.core.sirius.ui.internal.ControlResourceDialog controlResourceDialog =
          new org.polarsys.capella.core.sirius.ui.internal.ControlResourceDialog(_shell, domain, _eObject.eResource(), _eObject);
      return controlResourceDialog;
    }

    /**
     * @see org.eclipse.sirius.ui.tools.api.control.SiriusControlHandler#getDefaultCorrespondingAird(org.eclipse.emf.common.util.URI)
     */
    @Override
    protected URI getDefaultCorrespondingAird(URI semanticModelUri) {
      return semanticModelUri.trimFileExtension().appendFileExtension(
          AbstractPreferencesInitializer.getString(ICapellaPreferences.PREFERENCE_CAPELLA_AIRD_FRAGMENT_FILE_EXTENSION, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<DRepresentationDescriptor> getRepresentationDescriptorsToMove(Shell shell, Session session, EObject semanticRoot) throws InterruptedException {
      Collection<DRepresentationDescriptor> repDescriptors = collectExistingRepresentationDescriptors(session, semanticRoot);
      Collection<DRepresentationDescriptor> repDescriptorsToMove = null;
      if (repDescriptors.isEmpty()) {
          repDescriptorsToMove = repDescriptors;
      } else {
          repDescriptorsToMove = askUserWhichRepresentationToSplit(_shell, session, repDescriptors);
      }
      return repDescriptorsToMove;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("synthetic-access")
    @Override
    public void performControl(Shell shell, final EObject semanticRoot, IProgressMonitor monitor) {
      final Session session = SessionManager.INSTANCE.getSession(semanticRoot);
      if (session != null) {
        final URI semanticDest = getControledResourceURI(_shell, semanticRoot);
        if (semanticDest != null) {
          final Set<DRepresentationDescriptor> repDescriptors = new HashSet<DRepresentationDescriptor>(0);
          try {
            repDescriptors.addAll(getRepresentationDescriptorsToMove(_shell, session, semanticRoot));
          } catch (InterruptedException exception_p) {
            StringBuilder loggerMessage = new StringBuilder(".performControl(..) _ "); //$NON-NLS-1$
            __logger.warn(new EmbeddedMessage(loggerMessage.toString(), IReportManagerDefaultComponents.UI));
          }
          Collection<Resource> resources = new HashSet<Resource>(0);
          for (DRepresentationDescriptor repDescriptor : repDescriptors) {
            resources.add(repDescriptor.eResource());
          }
          // Collect resources that needs to be updated according to this fragmentation.
          resources.addAll(RepresentationHelper.collectDependentResources(semanticRoot));

          URI representationDest = getDefaultCorrespondingAird(semanticDest);
          // Disable resourceSetSync notification to avoid unload / reload of fragmented resources during the fragmentation.
          setResourceSetSyncNotificationEnabled(session, false);
          try {
            doExecuteCommand(semanticRoot, resources, new CapellaSiriusControlCommand(semanticRoot, semanticDest, repDescriptors, representationDest));
          } finally {
            Display.getCurrent().syncExec(new Runnable() {
              public void run() {
                // Re-enable the notification.
                setResourceSetSyncNotificationEnabled(session, true);
                saveSession(semanticRoot);
                Set<DRepresentation> representations = Sets.newHashSet();
                for (DRepresentationDescriptor repDesc : repDescriptors) {
                  representations.add(repDesc.getRepresentation());
                }
                SessionHelper.reloadEditors(session, representations);
              }
            });
          }
        }
      }
    }

    @Override
    public void dispose() {
      super.dispose();
      _shell = null;

      return;
    }

  }

  public class CapellaSiriusUncontrolHandler extends SiriusUncontrolHandler {
    /**
     * @param semanticRoot_p
     * @return
     */
    private Resource addUnreferencedRootSemanticResource(final EObject semanticRoot) {
      // Get the execution manager.
      final List<Resource> referencingElements = new ArrayList<Resource>(1);
      // Create a writable command to make sure computation is performed in a transactional way.
      ICommand resolveResourceCommand = new AbstractReadOnlyCommand() {

        @Override
        public boolean isReadOnly() {
          return false;
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
          referencingElements.add(semanticRoot.eResource());
        }
      };
      // Run the command.
      TransactionHelper.getExecutionManager(semanticRoot).execute(resolveResourceCommand);
      return referencingElements.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performUncontrol(final Shell shell, final EObject semanticRoot, IProgressMonitor monitor) {
      boolean uncontrolRepresentations = shouldUncontrolRepresentations(shell);
      CapellaSiriusUncontrolCommand vuc = new CapellaSiriusUncontrolCommand(semanticRoot, uncontrolRepresentations);
      // Use the container to fake the modified resource since this is container resource that needs to be saved.
      EObject parentContainer = EcoreUtil2.getResourceContainer(semanticRoot);
      Resource airdResource = vuc.getAirdResourceWithAnalysisOn(parentContainer);
      Collection<Resource> resources = new HashSet<Resource>(1);
      if (null != airdResource) {
        resources.add(airdResource);
      }

      final Session session = SessionManager.INSTANCE.getSession(semanticRoot);
      if (session != null) {
        Collection<Setting> settings = session.getSemanticCrossReferencer().getInverseReferences(semanticRoot);
        for (Setting setting : settings) {
          if ((setting != null) && ViewpointPackage.Literals.DANALYSIS__MODELS.equals(setting.getEStructuralFeature())) {
            if ((setting.getEObject() != null) && (setting.getEObject().eResource() != null)) {
              resources.add(setting.getEObject().eResource());
            }
          }
        }
      }

      // Collect resources that needs to be updated according to this unfragmentation.
      resources.addAll(RepresentationHelper.collectDependentResources(semanticRoot));
      // In case of the semantic root is not referenced we have to add it to the file list.
      if ((null != semanticRoot.eResource()) && !resources.contains(semanticRoot.eResource())) {
        resources.add(addUnreferencedRootSemanticResource(semanticRoot));
      }
      // Disable resourceSetSync notification to avoid unload / reload of fragmented resources during the unfragmentation.
      setResourceSetSyncNotificationEnabled(session, false);
      try {
        doExecuteCommand(parentContainer, resources, vuc);
      } finally {
        Display.getCurrent().syncExec(new Runnable() {
          public void run() {
            // Re-enable the notification.
            setResourceSetSyncNotificationEnabled(session, true);
            saveSession(semanticRoot);
            SessionHelper.reloadEditors(session);
          }
        });

      }
    }
  }

  private static final Logger __logger = ReportManagerRegistry.getInstance().subscribe(IReportManagerDefaultComponents.UI);

  /**
   * Create a new action to control the models.
   */
  public DesignerControlAction() {
    super();
  }

  /**
   * Do execute Control/UnControl command within an {@link AbstractNonDirtyingCommand}. Hence these commands are not available for undo/redo.<br>
   * In addition, {@link ResourceSetSync} is manually updated to reflect control( or uncontrol) commands that don't emit EMF notifications.<br>
   * @param semanticRoot_p
   * @param representationResources_p
   * @param realCommand_p
   */
  protected void doExecuteCommand(final EObject semanticRoot, final Collection<Resource> representationResources, final Command realCommand) {
    final Map<Resource, ResourceStatus> initialResourceWithStatus = new HashMap<Resource, ResourceStatus>(1);
    try {
      TransactionHelper.getExecutionManager(semanticRoot).execute(new AbstractNonDirtyingCommand() {
        /**
         * Change resource status for specified parameters.
         * @param resourceSetSync_p
         * @param resourceWithStatus_p
         * @param handleResource_p
         * @param handleResourceStatus_p
         */
        private void changeResourceSyncStatus(ResourceSetSync resourceSetSync, Map<Resource, ResourceStatus> resourceWithStatus, Resource handleResource,
            ResourceStatus handleResourceStatus) {
          // Store resource and its initial status if we need to rollback to initial state.
          resourceWithStatus.put(handleResource, handleResourceStatus);
          resourceSetSync.statusChanged(handleResource, handleResourceStatus, ResourceStatus.CHANGED /* new status */);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commandInterrupted() {
          commandRolledBack();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commandRolledBack() {
          restoreResourceSyncStatus(initialResourceWithStatus, ResourceSetSync.getOrInstallResourceSetSync(TransactionHelper.getEditingDomain(semanticRoot)));
        }

        /**
         * Restore specified resources in the map to their initial sync status.<br>
         * This method must be called when rollbacking and/or interrupting the command.
         */
        protected void restoreResourceSyncStatus(Map<Resource, ResourceStatus> initialResourceWithStatus, ResourceSetSync resourceSetSync) {
          // Restore all modified resources to their initial status.
          Iterator<Entry<Resource, ResourceStatus>> iterator = initialResourceWithStatus.entrySet().iterator();
          while (iterator.hasNext()) {
            Map.Entry<Resource, ResourceStatus> entry = iterator.next();
            resourceSetSync.statusChanged(entry.getKey(), ResourceStatus.CHANGED /* status set in command execution */, entry.getValue());
          }
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
          ResourceSetSync resourceSetSync = ResourceSetSync.getOrInstallResourceSetSync(TransactionHelper.getEditingDomain(semanticRoot));
          // Handle Semantic resource.
          changeResourceSyncStatus(resourceSetSync, initialResourceWithStatus, semanticRoot.eResource(),
              ResourceSetSync.getStatus(semanticRoot.eResource()));
          // Handle representations resources.
          for (Resource representationResource : representationResources) {
            changeResourceSyncStatus(resourceSetSync, initialResourceWithStatus, representationResource, ResourceSetSync.getStatus(representationResource));
          }
          // Try to make file writable (if necessary).
          try {
            Collection<IFile> filesToMakeWritable = new ArrayList<IFile>(initialResourceWithStatus.keySet().size());
            for (Resource currentResource : initialResourceWithStatus.keySet()) {
              filesToMakeWritable.add(EcoreUtil2.getFile(currentResource));
            }
            // Throws exception if an issue occurs.
            FileModificationPreCommitListener.makeFilesWritable(TransactionHelper.getEditingDomain(semanticRoot), filesToMakeWritable);
            realCommand.execute();
          } catch (AbortedTransactionException exception_p) {
            commandRolledBack();
          }
        }
      });
    } finally {
      // Clear the map since everything is OK or rollback.
      initialResourceWithStatus.clear();
    }
  }

  /**
   * Fragment
   * @param shell__p
   */
  protected void fragment(final Shell shell_) {
    SiriusControlHandler siriusControlHandler = new CapellaSiriusControlHandler(shell_);
    siriusControlHandler.performControl(shell_, _eObject, new NullProgressMonitor());

    return;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    final boolean controlling = (command == null);
    final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    if (controlling) {
      fragment(shell);
    } else {
      unFragment(shell);
    }
  }

  /**
   * Save session.
   * @param semanticRoot_p
   */
  protected void saveSession(final EObject semanticRoot) {
    // Force to save the session again to make sure all modifications are saved.
    final Session session = SessionManager.INSTANCE.getSession(semanticRoot);
    TransactionHelper.getExecutionManager(session).execute(new AbstractNonDirtyingCommand() {
      public void run() {
        session.save(new NullProgressMonitor());
      }
    });
  }

  /**
   * UnFragment
   * @param shell__p
   */
  protected void unFragment(final Shell shell_) {
    // Ask the end-user to confirm the uncontrol operation.
    final UncontrolMessageDialog confirmationDialog = new UncontrolMessageDialog(shell_, EObjectLabelProviderHelper.getText(_eObject));
    if (!(Window.OK == confirmationDialog.open())) {
      return;
    }
    SiriusUncontrolHandler siriusUncontrolHandler = new CapellaSiriusUncontrolHandler() {

      /**
       * @see org.eclipse.sirius.ui.tools.api.control.SiriusUncontrolHandler#shouldUncontrolRepresentations(org.eclipse.swt.widgets.Shell)
       */
      @Override
      protected boolean shouldUncontrolRepresentations(Shell shell) {
        return confirmationDialog.shouldUncontrolRepresentations();
      }
    };

    siriusUncontrolHandler.performUncontrol(shell_, _eObject, new NullProgressMonitor());

    return;
  }

  /**
   * Set whether or not the resourceSetSync related to current TED emits notifications to its clients.
   * @param notificationEnabled_p
   */
  protected void setResourceSetSyncNotificationEnabled(Session session, boolean notificationEnabled) {
    ResourceSetSync.getOrInstallResourceSetSync(TransactionHelper.getEditingDomain(session)).setNotificationIsRequired(notificationEnabled);
  }
}
