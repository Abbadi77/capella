/*******************************************************************************
 * Copyright (c) 2019 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *  
 * Contributors:
 *    Thales - initial API and implementation
 *******************************************************************************/
package org.polarsys.capella.test.diagram.filters.ju.pdfb;

import java.util.Arrays;
import java.util.List;

import org.polarsys.capella.core.sirius.analysis.constants.IFilterNameConstants;

public class CollapsePortsForPDFB extends FiltersForPDFB {

  @Override
  protected String getFilterName() {
    return IFilterNameConstants.FILTER_PDFB_COLLAPSE_PORTS;
  }

  @Override
  protected List<String> getFilteredObjetIDs() {
    return Arrays.asList(new String[] { FUNCTION_OUTPUT_PORT_1_ID, FUNCTION_OUTPUT_PORT_2_ID, FUNCTION_OUTPUT_PORT_3_ID,
        FUNCTION_INPUT_PORT_1_ID, FUNCTION_INPUT_PORT_2_ID, FUNCTION_INPUT_PORT_3_ID,
        FUNCTION_PORT_WITHOUT_EXCHANGES_ID, EXCHANGE_CATEGORY_ID });
  }

}
