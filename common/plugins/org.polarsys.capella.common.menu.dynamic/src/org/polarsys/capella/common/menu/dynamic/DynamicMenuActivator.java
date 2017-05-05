/*******************************************************************************
 * Copyright (c) 2006, 2017 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *  
 * Contributors:
 *    Thales - initial API and implementation
 *******************************************************************************/
package org.polarsys.capella.common.menu.dynamic;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 */
public class DynamicMenuActivator extends Plugin {
  /**
   * The shared instance
   */
  private static DynamicMenuActivator plugin;

  /**
   * {@inheritDoc}
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance
   * @return the shared instance
   */
  public static DynamicMenuActivator getDefault() {
    return plugin;
  }
}
