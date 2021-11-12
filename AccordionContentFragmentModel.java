package com.mysite.core.models;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code AccordionFaqModel} Sling Model used for the all the components.
 */
@ConsumerType
public interface AccordionContentFragmentModel {

    /**
     * Getter for checking editor template
     *
     * @return String content fragment FAQ Title
     */
    default String getFaqTitle() {
        throw new UnsupportedOperationException();
    }
}
