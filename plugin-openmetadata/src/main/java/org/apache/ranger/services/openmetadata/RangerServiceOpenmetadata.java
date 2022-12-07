/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.services.openmetadata;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.services.openmetadata.client.OpenmetadataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RangerServiceOpenmetadata
        extends RangerBaseService
{
    private static final Logger LOG = LoggerFactory.getLogger(RangerServiceOpenmetadata.class);

    public RangerServiceOpenmetadata()
    {
        super();
    }

    @Override
    public Map<String, Object> validateConfig()
    {

        LOG.debug("RangerServiceOpenmetadata.validateConfigs(): Service {}", getServiceName());

        OpenmetadataClient client = new OpenmetadataClient(getServiceName(), configs);
        Map<String, Object> response = client.validateConfig();

        LOG.debug("RangerServiceOpenmetadata.validateConfigs(): Response: {}", response);

        return response;
    }

    @Override
    public List<String> lookupResource(ResourceLookupContext context)
            throws Exception
    {
        List<String> response = new ArrayList<>();

        String userInput = context.getUserInput();
        String resource = context.getResourceName();
        Map<String, List<String>> resourceMap = context.getResources();

        LOG.debug("==> RangerServiceOpenmetadata.lookupResource(): context: {}, userInput: {}, resource: {}, resourceMap: {}", context, userInput, resource, resourceMap);

        // TODO

        LOG.debug("<== RangerServiceOpenmetadata.lookupResource(): Response: {}", response);

        return response;
    }

    @Override
    public List<RangerPolicy> getDefaultRangerPolicies()
            throws Exception
    {
        List<RangerPolicy> defaultRangerPolicies = super.getDefaultRangerPolicies();

        LOG.debug("==> RangerServiceOpenmetadata.getDefaultRangerPolicies(): defaultRangerPolicies: {}", defaultRangerPolicies);

        // TODO

        LOG.debug("<== RangerServiceOpenmetadata.getDefaultRangerPolicies(): Response: {}", defaultRangerPolicies);

        return defaultRangerPolicies;
    }
}
