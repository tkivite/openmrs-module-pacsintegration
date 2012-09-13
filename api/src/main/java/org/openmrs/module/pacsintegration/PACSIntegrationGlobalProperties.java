/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.pacsintegration;

import org.openmrs.api.context.Context;

public class PACSIntegrationGlobalProperties {

    public static final String GLOBAL_PROPERTY_MIRTH_IP_ADDRESS() {
        return Context.getAdministrationService().getGlobalProperty("pacsintegration.mirthIpAddress");
    }

    public static final Integer GLOBAL_PROPERTY_MIRTH_INPUT_PORT() {
        return Integer.parseInt(Context.getAdministrationService().getGlobalProperty("pacsintegration.mirthInputPort"));
    }

}
