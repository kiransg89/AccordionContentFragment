package com.mysite.core.models;

import java.util.Calendar;
import java.util.Date;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code CFModel} Sling Model used for the all the components.
 */
@ConsumerType
public interface CFModel {

    /**
     * Getter for UXDIA Article Content Fragment
     * @param elementName
     *
     * @return String get element by name
     */
    default String getElementContent(String elementName) {
        throw new UnsupportedOperationException();
    }
}
