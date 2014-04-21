/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Helper class that allows putting (almost) arbitrary objects into Activiti processes.
 *
 * Generally, prism objects and containers and jaxb objects are stored in their XML form,
 * allowing for safe deserialization in potentially newer version of midpoint.
 *
 * Other serializable items are stored as such.
 *
 * There's a child class (JaxbValueContainer) that allows directly retrieving XML representation of the object
 * (if there's one).
 *
 * @author mederly
 */
public class SerializationSafeContainer<T> implements Serializable {

    private static final long serialVersionUID = 7269803380754945968L;

    private static final Trace LOGGER = TraceManager.getTrace(SerializationSafeContainer.class);
    public static final int MAX_WIDTH = 500;

    // this is the actual (directly usable) value of the item
    private transient T actualValue;

    // if there's no need to encode the value, it is stored in the first attribute
    // if there is (e.g. for PrismObjects) the encoded value is stored in the second attribute
    private T valueForStorageWhenNotEncoded;
    protected String valueForStorageWhenEncoded;

    // beware, for JAXB, PRISM_OBJECT and PRISM_CONTAINER encoding schemes the value must be XML, as it might be
    // exposed through JaxbValueContainer
    protected EncodingScheme encodingScheme;

    private transient PrismContext prismContext;

    public SerializationSafeContainer(T value, PrismContext prismContext) {
        Validate.notNull(prismContext, "prismContext must not be null");
        this.prismContext = prismContext;
        setValue(value);
    }

    public void setValue(T value) {
        this.actualValue = value;

        checkPrismContext();
        if (value != null && prismContext.canSerialize(value)) {
            try {
                this.valueForStorageWhenEncoded = prismContext.serializeAnyData(value, PrismContext.LANG_XML);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't serialize value of type " + value.getClass() + ": " + e.getMessage(), e);
            }
            this.valueForStorageWhenNotEncoded = null;
            encodingScheme = EncodingScheme.PRISM;
        } else if (value == null || value instanceof Serializable) {
            this.valueForStorageWhenNotEncoded = value;
            this.valueForStorageWhenEncoded = null;
            encodingScheme = EncodingScheme.NONE;
            if (value instanceof Itemable) {
                throw new IllegalStateException("Itemable value is used as not-encoded serializable item; value = " + value);
            }
        } else {
            throw new IllegalStateException("Attempt to put non-serializable item " + value.getClass() + " into " + this.getClass().getSimpleName());
        }
    }

    private void checkPrismContext() {
        Validate.notNull(prismContext, "In SerializationSafeContainer the prismContext is not set up");
    }

    public T getValue() {

        if (actualValue != null) {
            return actualValue;
        }

        if (valueForStorageWhenNotEncoded != null) {
            actualValue = valueForStorageWhenNotEncoded;
            return actualValue;
        }

        if (valueForStorageWhenEncoded != null) {
            if (prismContext == null) {
                throw new IllegalStateException("PrismContext not set for SerializationSafeContainer holding " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH));
            }

            if (encodingScheme == EncodingScheme.PRISM) {
                try {
                    actualValue = (T) prismContext.parseAnyData(valueForStorageWhenEncoded, PrismContext.LANG_XML);
                    if (actualValue instanceof Item) {
                        Item item = (Item) actualValue;
                        if (item.isEmpty()) {
                            actualValue = null;
                        } else if (item.size() == 1) {
                            PrismValue itemValue = (PrismValue) item.getValues().get(0);
                            if (itemValue instanceof PrismContainerValue) {
                                actualValue = (T) ((PrismContainerValue) itemValue).asContainerable();
                            } else if (itemValue instanceof PrismPropertyValue) {
                                actualValue = (T) ((PrismPropertyValue) itemValue).getValue();
                            } else if (itemValue instanceof PrismReferenceValue) {
                                actualValue = (T) itemValue;   // TODO: ok???
                            } else {
                                throw new SchemaException("Unknown itemValue: " + itemValue);
                            }
                        } else {
                            throw new SchemaException("More than one value after deserialization of " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH) + ": " + item.getValues().size() + " values");
                        }
                    }
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't deserialize value from JAXB: " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH), e);
                }
                return actualValue;
            } else {
                throw new IllegalStateException("Unexpected encoding scheme " + encodingScheme);
            }
        }

        return null;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    // for testing purposes
    public void clearActualValue() {
        actualValue = null;
    }

    public enum EncodingScheme { PRISM, NONE };

    @Override
    public String toString() {
        return "SerializationSafeContainer{" +
                "actualValue " + (actualValue != null ? "SET" : "NOT SET") +
                ", valueForStorageWhenNotEncoded=" + valueForStorageWhenNotEncoded +
                ", valueForStorageWhenEncoded='" + valueForStorageWhenEncoded + '\'' +
                ", encodingScheme=" + encodingScheme +
                ", prismContext " + (prismContext != null ? "SET" : "NOT SET") +
                '}';
    }
}
