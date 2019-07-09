/*******************************************************************************
 * Copyright (c) 2006, 2019 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *  
 * Contributors:
 *    Thales - initial API and implementation
 *******************************************************************************/
package org.polarsys.capella.core.business.queries.queries.ctx;

import java.util.ArrayList;
import java.util.List;

import org.polarsys.capella.common.queries.AbstractQuery;
import org.polarsys.capella.common.queries.queryContext.IQueryContext;
import org.polarsys.capella.core.data.capellacore.CapellaElement;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.cs.Component;
import org.polarsys.capella.core.model.helpers.BlockArchitectureExt;
import org.polarsys.capella.core.model.helpers.StateMachineExt;
import org.polarsys.capella.core.model.helpers.SystemEngineeringExt;

public class GetAvailable_Capability_AvailableInStates extends AbstractQuery {

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public List<Object> execute(Object input, IQueryContext context) {
    CapellaElement capellaElement = (CapellaElement) input;
    List<Object> availableElements = new ArrayList<Object>(1);
    BlockArchitecture blockArchitecture = SystemEngineeringExt.getRootBlockArchitecture(capellaElement);
    for (Component component : BlockArchitectureExt.getAllComponents(blockArchitecture)) {
      availableElements.addAll(StateMachineExt.getElementsFromBlockArchitecture(component, capellaElement));
    }
    return (List) availableElements;
  }
  
}