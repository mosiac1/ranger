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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.services.openmetadata.client.OpenmetadataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        for (RangerPolicy policy : defaultRangerPolicies) {
            // We assume that default creation will put all the correct access types in the first policy item
            List<RangerPolicy.RangerPolicyItemAccess> resourceAccessTypes = policy.getPolicyItems().get(0).getAccesses();

            ImmutableList.Builder<RangerPolicy.RangerPolicyItem> policyItemBuilder = ImmutableList.builder();

            // Data Consumer policy item
            RangerPolicy.RangerPolicyItem viewPolicyItem = createRangerPolicyItem("data-consumer", resourceAccessTypes,
                    List.of("ViewAll", "EditDescription", "EditTags"));
            policyItemBuilder.add(viewPolicyItem);

            // Data Steward policy item
            RangerPolicy.RangerPolicyItem dataStewardPolicyItem = createRangerPolicyItem("data-steward", resourceAccessTypes,
                    List.of("ViewAll", "EditDescription", "EditDisplayName","EditLineage","EditOwner", "EditTags"));
            policyItemBuilder.add(dataStewardPolicyItem);

            // Bot policy item
            RangerPolicy.RangerPolicyItem botPolicyItem;
            // Bots are not allowed to create new bots or webhooks
            if (policy.getResources().containsKey("bot") && policy.getResources().containsKey("webhook")) {
                botPolicyItem = createRangerPolicyItem("bot", resourceAccessTypes, null, List.of("create", "delete"));
            }
            // Otherwise they have admin level access
            else {
                botPolicyItem = createRangerPolicyItem("bot", resourceAccessTypes);
            }
            if (StringUtils.isNotEmpty(lookUpUser)) {
                botPolicyItem.setUsers(List.of(lookUpUser));
            }
            policyItemBuilder.add(botPolicyItem);


            // Admin policy item
            RangerPolicy.RangerPolicyItem adminPolicyItem = createRangerPolicyItem("admin", resourceAccessTypes);
            adminPolicyItem.setUsers(List.of("admin"));
            policyItemBuilder.add(adminPolicyItem);

            policy.setPolicyItems(policyItemBuilder.build());
        }

        LOG.debug("<== RangerServiceOpenmetadata.getDefaultRangerPolicies(): Response: {}", defaultRangerPolicies);

        return defaultRangerPolicies;
    }


    private RangerPolicy.RangerPolicyItem createRangerPolicyItem(String roleName, List<RangerPolicy.RangerPolicyItemAccess> allAccessTypes) {
       return createRangerPolicyItem(roleName, allAccessTypes, null, null);
    }

    private RangerPolicy.RangerPolicyItem createRangerPolicyItem(String roleName, List<RangerPolicy.RangerPolicyItemAccess> allAccessTypes, List<String> requestedAccessTypes) {
        return createRangerPolicyItem(roleName, allAccessTypes, requestedAccessTypes, null);
    }

    private RangerPolicy.RangerPolicyItem createRangerPolicyItem(String roleName, List<RangerPolicy.RangerPolicyItemAccess> allAccessTypes, List<String> requestedAccessTypes, List<String> filteredAccessTypes) {
        RangerPolicy.RangerPolicyItem policyItem = new RangerPolicy.RangerPolicyItem();

        Set<String> accessTypesResult = allAccessTypes.stream().map(RangerPolicy.RangerPolicyItemAccess::getType).collect(Collectors.toSet());

        if (requestedAccessTypes != null) {
            accessTypesResult.retainAll(requestedAccessTypes);
        }

        if (filteredAccessTypes != null) {
            filteredAccessTypes.forEach(accessTypesResult::remove);
        }

        List<RangerPolicy.RangerPolicyItemAccess> rangerPolicyItemAccesses = accessTypesResult.stream().map(s -> new RangerPolicy.RangerPolicyItemAccess(s, true)).collect(Collectors.toList());

        policyItem.setRoles(List.of(String.format("%s-%s", serviceName, roleName)));
        policyItem.setUsers(List.of());
        policyItem.setGroups(List.of());
        policyItem.setAccesses(rangerPolicyItemAccesses);
        policyItem.setDelegateAdmin(false);
        policyItem.setConditions(List.of());
        return policyItem;
    }
}
