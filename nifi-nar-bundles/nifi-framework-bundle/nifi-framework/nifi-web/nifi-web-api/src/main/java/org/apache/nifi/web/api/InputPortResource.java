/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.web.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.annotations.Authorization;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.authorization.Authorizer;
import org.apache.nifi.authorization.RequestAction;
import org.apache.nifi.authorization.resource.Authorizable;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.Revision;
import org.apache.nifi.web.api.dto.PortDTO;
import org.apache.nifi.web.api.entity.PortEntity;
import org.apache.nifi.web.api.request.ClientIdParameter;
import org.apache.nifi.web.api.request.LongParameter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * RESTful endpoint for managing an Input Port.
 */
@Path("/input-ports")
@Api(
    value = "/input-ports",
    description = "Endpoint for managing an Input Port."
)
public class InputPortResource extends ApplicationResource {

    private NiFiServiceFacade serviceFacade;
    private Authorizer authorizer;

    /**
     * Populates the uri for the specified input ports.
     *
     * @param inputPortEntites ports
     * @return ports
     */
    public Set<PortEntity> populateRemainingInputPortEntitiesContent(Set<PortEntity> inputPortEntites) {
        for (PortEntity inputPortEntity : inputPortEntites) {
            populateRemainingInputPortEntityContent(inputPortEntity);
        }
        return inputPortEntites;
    }

        /**
         * Populates the uri for the specified input port.
         *
         * @param inputPortEntity port
         * @return ports
         */
    public PortEntity populateRemainingInputPortEntityContent(PortEntity inputPortEntity) {
        if (inputPortEntity.getComponent() != null) {
            populateRemainingInputPortContent(inputPortEntity.getComponent());
        }
        return inputPortEntity;
    }

    /**
     * Populates the uri for the specified input ports.
     *
     * @param inputPorts ports
     * @return ports
     */
    public Set<PortDTO> populateRemainingInputPortsContent(Set<PortDTO> inputPorts) {
        for (PortDTO inputPort : inputPorts) {
            populateRemainingInputPortContent(inputPort);
        }
        return inputPorts;
    }

    /**
     * Populates the uri for the specified input ports.
     */
    public PortDTO populateRemainingInputPortContent(PortDTO inputPort) {
        // populate the input port uri
        inputPort.setUri(generateResourceUri("input-ports", inputPort.getId()));
        return inputPort;
    }

    /**
     * Retrieves the specified input port.
     *
     * @param id The id of the input port to retrieve
     * @return A inputPortEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    // TODO - @PreAuthorize("hasAnyRole('ROLE_MONITOR', 'ROLE_DFM', 'ROLE_ADMIN')")
    @ApiOperation(
            value = "Gets an input port",
            response = PortEntity.class,
            authorizations = {
                @Authorization(value = "Read Only", type = "ROLE_MONITOR"),
                @Authorization(value = "Data Flow Manager", type = "ROLE_DFM"),
                @Authorization(value = "Administrator", type = "ROLE_ADMIN")
            }
    )
    @ApiResponses(
            value = {
                @ApiResponse(code = 400, message = "NiFi was unable to complete the request because it was invalid. The request should not be retried without modification."),
                @ApiResponse(code = 401, message = "Client could not be authenticated."),
                @ApiResponse(code = 403, message = "Client is not authorized to make this request."),
                @ApiResponse(code = 404, message = "The specified resource could not be found."),
                @ApiResponse(code = 409, message = "The request was valid but NiFi was not in the appropriate state to process it. Retrying the same request later may be successful.")
            }
    )
    public Response getInputPort(
            @ApiParam(
                    value = "The input port id.",
                    required = true
            )
            @PathParam("id") final String id) {

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        // authorize access
        serviceFacade.authorizeAccess(lookup -> {
            final Authorizable inputPort = lookup.getInputPort(id);
            inputPort.authorize(authorizer, RequestAction.READ);
        });

        // get the port
        final PortEntity entity = serviceFacade.getInputPort(id);
        populateRemainingInputPortEntityContent(entity);

        return clusterContext(generateOkResponse(entity)).build();
    }

    /**
     * Updates the specified input port.
     *
     * @param httpServletRequest request
     * @param id The id of the input port to update.
     * @param portEntity A inputPortEntity.
     * @return A inputPortEntity.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    // TODO - @PreAuthorize("hasRole('ROLE_DFM')")
    @ApiOperation(
            value = "Updates an input port",
            response = PortEntity.class,
            authorizations = {
                @Authorization(value = "Data Flow Manager", type = "ROLE_DFM")
            }
    )
    @ApiResponses(
            value = {
                @ApiResponse(code = 400, message = "NiFi was unable to complete the request because it was invalid. The request should not be retried without modification."),
                @ApiResponse(code = 401, message = "Client could not be authenticated."),
                @ApiResponse(code = 403, message = "Client is not authorized to make this request."),
                @ApiResponse(code = 404, message = "The specified resource could not be found."),
                @ApiResponse(code = 409, message = "The request was valid but NiFi was not in the appropriate state to process it. Retrying the same request later may be successful.")
            }
    )
    public Response updateInputPort(
            @Context HttpServletRequest httpServletRequest,
            @ApiParam(
                    value = "The input port id.",
                    required = true
            )
            @PathParam("id") final String id,
            @ApiParam(
                    value = "The input port configuration details.",
                    required = true
            ) final PortEntity portEntity) {

        if (portEntity == null || portEntity.getComponent() == null) {
            throw new IllegalArgumentException("Input port details must be specified.");
        }

        if (portEntity.getRevision() == null) {
            throw new IllegalArgumentException("Revision must be specified.");
        }

        // ensure the ids are the same
        final PortDTO requestPortDTO = portEntity.getComponent();
        if (!id.equals(requestPortDTO.getId())) {
            throw new IllegalArgumentException(String.format("The input port id (%s) in the request body does not equal the "
                    + "input port id of the requested resource (%s).", requestPortDTO.getId(), id));
        }

        if (isReplicateRequest()) {
            return replicate(HttpMethod.PUT, portEntity);
        }

        // handle expects request (usually from the cluster manager)
        final Revision revision = getRevision(portEntity, id);
        return withWriteLock(
            serviceFacade,
            revision,
            lookup -> {
                Authorizable authorizable = lookup.getInputPort(id);
                authorizable.authorize(authorizer, RequestAction.WRITE);
            },
            () -> serviceFacade.verifyUpdateInputPort(requestPortDTO),
            () -> {
                // update the input port
                final PortEntity entity = serviceFacade.updateInputPort(revision, requestPortDTO);
                populateRemainingInputPortEntityContent(entity);

                return clusterContext(generateOkResponse(entity)).build();
            }
        );
    }

    /**
     * Removes the specified input port.
     *
     * @param httpServletRequest request
     * @param version The revision is used to verify the client is working with the latest version of the flow.
     * @param clientId Optional client id. If the client id is not specified, a new one will be generated. This value (whether specified or generated) is included in the response.
     * @param id The id of the input port to remove.
     * @return A inputPortEntity.
     */
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    // TODO - @PreAuthorize("hasRole('ROLE_DFM')")
    @ApiOperation(
            value = "Deletes an input port",
            response = PortEntity.class,
            authorizations = {
                @Authorization(value = "Data Flow Manager", type = "ROLE_DFM")
            }
    )
    @ApiResponses(
            value = {
                @ApiResponse(code = 400, message = "NiFi was unable to complete the request because it was invalid. The request should not be retried without modification."),
                @ApiResponse(code = 401, message = "Client could not be authenticated."),
                @ApiResponse(code = 403, message = "Client is not authorized to make this request."),
                @ApiResponse(code = 404, message = "The specified resource could not be found."),
                @ApiResponse(code = 409, message = "The request was valid but NiFi was not in the appropriate state to process it. Retrying the same request later may be successful.")
            }
    )
    public Response removeInputPort(
            @Context HttpServletRequest httpServletRequest,
            @ApiParam(
                    value = "The revision is used to verify the client is working with the latest version of the flow.",
                    required = false
            )
            @QueryParam(VERSION) final LongParameter version,
            @ApiParam(
                    value = "If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.",
                    required = false
            )
            @QueryParam(CLIENT_ID) @DefaultValue(StringUtils.EMPTY) final ClientIdParameter clientId,
            @ApiParam(
                    value = "The input port id.",
                    required = true
            )
            @PathParam("id") final String id) {

        if (isReplicateRequest()) {
            return replicate(HttpMethod.DELETE);
        }

        // handle expects request (usually from the cluster manager)
        final Revision revision = new Revision(version == null ? null : version.getLong(), clientId.getClientId(), id);
        return withWriteLock(
            serviceFacade,
            revision,
            lookup -> {
                final Authorizable inputPort = lookup.getInputPort(id);
                inputPort.authorize(authorizer, RequestAction.WRITE);
            },
            () -> serviceFacade.verifyDeleteInputPort(id),
            () -> {
                // delete the specified input port
                final PortEntity entity = serviceFacade.deleteInputPort(revision, id);
                return clusterContext(generateOkResponse(entity)).build();
            }
        );
    }

    // setters
    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public void setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
    }
}
