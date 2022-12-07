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
package org.apache.ranger.services.openmetadata.client;

import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.client.HadoopException;
import org.openmetadata.client.api.CatalogApi;
import org.openmetadata.client.api.ServicesApi;
import org.openmetadata.client.api.UtilApi;
import org.openmetadata.client.gateway.OpenMetadata;
import org.openmetadata.client.model.EntitiesCount;
import org.openmetadata.client.model.OpenMetadataServerVersion;
import org.openmetadata.schema.security.client.OpenMetadataJWTClientConfig;
import org.openmetadata.schema.services.connections.metadata.OpenMetadataServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OpenmetadataClient
        extends BaseClient
        implements Cloneable
{
    private static final Logger LOG = LoggerFactory.getLogger(OpenmetadataClient.class);

    private static final String ERR_MSG = "You can still save the repository and start creating policies, but you would not be able to use autocomplete for resource names. Check ranger_admin.log for more info.";

    private OpenMetadata client;

    public OpenmetadataClient(String svcName, Map<String, String> connectionProperties)
    {
        super(svcName, connectionProperties);
        init();
    }

    public OpenmetadataClient(String serviceName, Map<String, String> connectionProperties, String defaultConfigFile)
    {
        super(serviceName, connectionProperties, defaultConfigFile);
        init();
    }

    private void init()
    {
        Subject.doAs(getLoginSubject(), (PrivilegedAction<?>) () -> {

            Properties prop = getConfigHolder().getRangerSection();
            String endpoint = prop.getProperty("endpoint");
            String token = prop.getProperty("token");

            LOG.debug("==> Openmetadata.init(): Found configuration endpoint: {}, token: {} from properties: {}", endpoint, token, prop);

            try {
                OpenMetadataServerConnection server = new OpenMetadataServerConnection();
                server.setHostPort(endpoint);
                server.setApiVersion("v1");
                server.setAuthProvider(OpenMetadataServerConnection.AuthProvider.OPENMETADATA);
                OpenMetadataJWTClientConfig openMetadataJWTClientConfig = new OpenMetadataJWTClientConfig();
                openMetadataJWTClientConfig.setJwtToken(token);
                server.setSecurityConfig(openMetadataJWTClientConfig);
                client = new OpenMetadata(server);
            }
            catch (Throwable e) {
                LOG.error("==> OpenmetadataClient.init(): Error creating OpenMetadata client", e);
                String msgDsc = String.format("initConnection: unable to create OpenMetadata client for endpoint %s", endpoint);
                HadoopException hadoopException = new HadoopException(msgDsc, e);
                hadoopException.generateResponseDataMap(false, getMessage(e), msgDsc + ERR_MSG, null, null);
                throw hadoopException;
            }

            return null;
        });

        LOG.debug("==> Openmetadata.init(): Initialized successfully");
    }

    public Map<String, Object> validateConfig()
    {
        LOG.debug("==> OpenmetadataClient.validateConfig() serviceName: {}, connectionProperties: {}", getSerivceName(), connectionProperties);

        OpenMetadataServerVersion catalogVersion;
        EntitiesCount entitiesCount;

        try {
            CatalogApi catalogApi = client.buildClient(CatalogApi.class);
            catalogVersion = catalogApi.getCatalogVersion();
        }
        catch (Throwable e) {
            LOG.error("==> OpenmetadataClient.validateConfig(): Error: cannot fetch OpenMetadata version", e);
            String msgDsc = String.format("validateConfig: cannot fetch OpenMetadata version for %s", getSerivceName());
            HadoopException hadoopException = new HadoopException(msgDsc, e);
            hadoopException.generateResponseDataMap(false, getMessage(e), msgDsc + ERR_MSG, null, null);
            throw hadoopException;
        }

        try {
            UtilApi utilApi = client.buildClient(UtilApi.class);
            entitiesCount = utilApi.listEntitiesCount();
        }
        catch (Throwable e) {
            LOG.error("==> OpenmetadataClient.validateConfig(): Error: cannot fetch OpenMetadata entity counts", e);
            String msgDsc = String.format("validateConfig: cannot fetch OpenMetadata entity counts for %s", getSerivceName());
            HadoopException hadoopException = new HadoopException(msgDsc, e);
            hadoopException.generateResponseDataMap(false, getMessage(e), msgDsc + ERR_MSG, null, null);
            throw hadoopException;
        }

        String msg = String.format("Connection test successful. Server version: %s. Entity counts: %s.", catalogVersion, entitiesCount);
        Map<String, Object> response = new HashMap<>();
        generateResponseDataMap(true, msg, msg, null, null, response);

        LOG.debug("<== OpenmetadataClient.connectionTest() Response: {}", response);

        return response;
    }
}
