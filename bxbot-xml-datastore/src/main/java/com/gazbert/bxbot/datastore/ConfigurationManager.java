/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.datastore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;

/**
 * The generic configuration manager loads config from a given XML config file.
 *
 * @author gazbert
 */
public final class ConfigurationManager {

    private static final Logger LOG = LogManager.getLogger();
    private final static Object MUTEX = new Object();

    private ConfigurationManager() {
    }

    /*
     * Loads and returns the requested configuration.
     */
    public static <T> T loadConfig(Class<T> configClass, String xmlConfigFile, String xmlSchemaFile) {

        LOG.info(() -> "Loading configuration for [" + configClass + "] from: " + xmlConfigFile + " ...");

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(configClass.getPackage().getName());
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // optional schema validation
            if (xmlSchemaFile != null) {
                final InputStream xsdStream = ConfigurationManager.class.getClassLoader().getResourceAsStream(xmlSchemaFile);
                final StreamSource xsdSource = new StreamSource(xsdStream);
                final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                final Schema schema = sf.newSchema(xsdSource);
                unmarshaller.setSchema(schema);
            }

            synchronized (MUTEX) {
                final FileInputStream fileInputStream = new FileInputStream(xmlConfigFile);
                final JAXBElement<?> requestedConfigRootXmlElement = (JAXBElement<?>) unmarshaller.unmarshal(fileInputStream);
                final T requestedConfig = (T) requestedConfigRootXmlElement.getValue();
                fileInputStream.close();

                LOG.info(() -> "Loaded and set configuration for [" + configClass + "] successfully!");
                return requestedConfig;
            }

        } catch (JAXBException | SAXException e) {
            final String errorMsg = "Failed to load [" + xmlConfigFile + "] file and validate it using XML Schema [" + xmlSchemaFile + "]";
            LOG.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        } catch (IOException e) {
            final String errorMsg = "Failed to find or read [" + xmlConfigFile + "] config";
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /*
     * Saves given config to filesystem.
     */
    public static <T> void saveConfig(Class<T> configClass, T config, String xmlConfigFile) {

        LOG.info(() -> "Saving configuration for [" + configClass + "] to: " + xmlConfigFile + " ...");

        try {
            final JAXBContext context = JAXBContext.newInstance(config.getClass());
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            synchronized (MUTEX) {
                final FileOutputStream fileOutputStream = new FileOutputStream(xmlConfigFile);
                marshaller.marshal(config, fileOutputStream);
                fileOutputStream.close();
            }

        } catch (JAXBException e) {
            final String errorMsg = "Failed to save config to [" + xmlConfigFile + "] file.";
            LOG.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        } catch (IOException e) {
            final String errorMsg = "Failed to find or read [" + xmlConfigFile + "] config";
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }
}
