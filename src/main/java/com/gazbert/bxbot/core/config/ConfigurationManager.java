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

package com.gazbert.bxbot.core.config;

import com.gazbert.bxbot.core.util.LogUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


/*
 * The generic configuration manager loads config from a given XML config file.
 */
public final class ConfigurationManager {

    private static final Logger LOG = Logger.getLogger(ConfigurationManager.class);

    private ConfigurationManager() {
    }

    /*
     * Loads and returns the requested configuration.
     */
    public static <T> T loadConfig(Class<T> configClass, String xmlConfigFile, String xmlSchemaFile) {

        LogUtils.log(LOG, Level.INFO, () -> "Loading configuration for [" + configClass + "] from: " + xmlConfigFile + " ...");

        try {

            final JAXBContext jaxbContext = JAXBContext.newInstance(configClass.getPackage().getName());
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // optional schema validation
            if (xmlSchemaFile != null) {
                final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                final Schema schema = sf.newSchema(new File(xmlSchemaFile));
                unmarshaller.setSchema(schema);
            }

            // unmarshal XML config into Java world
            final JAXBElement<?> requestedConfigRootXmlElement = (JAXBElement<?>) unmarshaller.unmarshal(
                    new FileInputStream(xmlConfigFile));

            final T requestedConfig = (T) requestedConfigRootXmlElement.getValue();
            LogUtils.log(LOG, Level.INFO, () -> "Loaded and set configuration for [" + configClass + "] successfully!");

            return requestedConfig;

        } catch (JAXBException | SAXException e) {
            final String errorMsg = "Failed to load [" + xmlConfigFile + "] file and validate it using XML Schema [" + xmlSchemaFile + "]";
            LOG.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        } catch (FileNotFoundException e) {
            final String errorMsg = "Failed to find or read [" + xmlConfigFile + "] config";
            LOG.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }
}
