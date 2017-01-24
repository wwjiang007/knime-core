/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   16.02.2015 (thor): created
 */
package org.knime.core.jaxrs.providers.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
@Consumes({MediaType.APPLICATION_JSON})
@Provider
public class GenericJSONDeserializer implements MessageBodyReader<Object> {
    private static final boolean ASSERTIONS_ENABLED;

    static {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true; // assignment is intended
        ASSERTIONS_ENABLED = assertionsEnabled;
    }

    private final ObjectMapper m_jsonMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object readFrom(final Class<Object> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
        final InputStream entityStream) throws IOException, WebApplicationException {

        ObjectReader reader = m_jsonMapper.readerFor(type);
        if (!ASSERTIONS_ENABLED) {
            // this ensures backwards compatibility with "old" readers if new fields are added
            reader = reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }

        JsonParser jp = reader.getFactory().createParser(entityStream);
        jp.disable(Feature.AUTO_CLOSE_SOURCE);
        if (jp.nextToken() == null) {
            throw new IOException("Input does not contain any content");
        } else {
            try {
                return reader.readValue(jp);
            } catch (JsonParseException ex) {
                Response response =
                    Response.status(Status.BAD_REQUEST).entity(ex.getMessage() + "\n").type("text/plain").build();

                throw new WebApplicationException(ex, response);
            }
        }
    }
}