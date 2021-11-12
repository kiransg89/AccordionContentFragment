package com.mysite.core.models.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.mysite.core.models.AccordionContentFragmentModel;
import com.mysite.core.models.CFModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Model(adaptables = SlingHttpServletRequest.class, adapters = {AccordionContentFragmentModel.class})
public class AccordionContentFragmentModelImpl implements AccordionContentFragmentModel {

    private static final Logger LOG = LoggerFactory.getLogger(AccordionContentFragmentModelImpl.class);

    private static final String CF_FRAGMENT_PATH = "fragmentPath";
    private static final String CF_ELEMENTS_NAME = "elementNames";
    private static final String CF_FAQ_QUESTION = "question";
    private static final String CF_FAQ_ANSWER = "answer";
    private static final String CF_FAQ_ITEMS = "/item_";
    private static final String CF_FAQ_PANEL_TITLE = "cq:panelTitle";

    private static final String CF_ARTICLE_FRAGMENT_PATH = "articleFragmentPath";
    private static final String CF_FAQ_ITEM = "item_";
    private static final String UNDERSCORE = "_";
    private static final String PUBLISH_ENV = "publish";

    private static final Map<Integer, String> NUMBERS = new HashMap<>();
    static {
        NUMBERS.put(1, "One");
        NUMBERS.put(2, "Two");
        NUMBERS.put(3, "Three");
        NUMBERS.put(4, "Four");
        NUMBERS.put(5, "Five");
        NUMBERS.put(6, "Six");
        NUMBERS.put(7, "Seven");
        NUMBERS.put(8, "Eight");
        NUMBERS.put(9, "Nine");
        NUMBERS.put(10, "Ten");
    }

    static Map<String, Object> defualtFaqProperties = new HashMap<>();
    static {
        defualtFaqProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        defualtFaqProperties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "uxdia/components/content/articlecontentfragment");
        defualtFaqProperties.put("containerOpted", "dynamic");
        defualtFaqProperties.put("displayMode", "singleText");
        defualtFaqProperties.put("paragraphScope", "all");
        defualtFaqProperties.put("variationName", "master");
    }

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private SlingSettingsService slingSettings;

    @OSGiService
    private ModelFactory modelFactory;

    private List<Integer> faqCount = new ArrayList<>();

    @PostConstruct
    private void initModel() {
        if(!isPublish(slingSettings)) {
            String pageArticleFragmentPath = currentPage.getProperties().get(CF_ARTICLE_FRAGMENT_PATH, StringUtils.EMPTY);
            if(StringUtils.isNotEmpty(pageArticleFragmentPath)) {
                Resource contentFragmentResource = resourceResolver.resolve(pageArticleFragmentPath);
                CFModel cfModel = modelFactory.getModelFromWrappedRequest(request, contentFragmentResource, CFModel.class);
                getBodyElementCounts(cfModel);
                List<Integer> childrens = StreamSupport.stream(resource.getChildren().spliterator(), false)
                        .map(accRes -> StringUtils.substringAfter(accRes.getName(), UNDERSCORE))
                        .map(NumberUtils::toInt)
                        .sorted()
                        .collect(Collectors.toList());
                if(!faqCount.equals(childrens)) {
                    deleteInvalidResource(childrens);
                    addFaqItem(faqCount, resourceResolver, resource, pageArticleFragmentPath, cfModel);
                }
            }
        }
    }

    private void deleteInvalidResource(List<Integer> childrens) {
        childrens.removeAll(faqCount);
        if(!childrens.isEmpty()) {
            childrens.stream().forEach(this::deleteResource);
            commitChanges();
        }
    }
    private void commitChanges() {
        if(resourceResolver.hasChanges()) {
            try {
                resourceResolver.commit();
            } catch (PersistenceException e) {
                LOG.error("Unable save the cahnges {}",e.getMessage());
            }
        }
    }

    private void deleteResource(Integer num) {
        try {
            resourceResolver.delete(resource.getChild(CF_FAQ_ITEM+num));
        } catch (PersistenceException e) {
            LOG.error("Unable to delete the resource {}", e.getMessage());
        }
    }

    public void getBodyElementCounts(CFModel cfModel) {
        faqCount = IntStream.rangeClosed(1, 20).filter(num -> checkFragment(num, cfModel, CF_FAQ_QUESTION)).boxed().collect(Collectors.toList());
    }

    public static boolean checkFragment(int num, CFModel cfModel, String elementName) {
        return StringUtils.isNotEmpty(cfModel.getElementContent(elementName+ NUMBERS.get(num)));
    }

    public static void addFaqItem(List<Integer> faqCount, ResourceResolver resourceResolver, Resource resource, String pageArticleFragmentPath, CFModel cfModel) {
        while(!faqCount.isEmpty()) {
            try {
                Map<String, Object> faqProperties = defualtFaqProperties;
                faqProperties.put(CF_FRAGMENT_PATH, pageArticleFragmentPath);
                faqProperties.put(CF_ELEMENTS_NAME, CF_FAQ_ANSWER+ NUMBERS.get(faqCount.get(0)));
                faqProperties.put(CF_FAQ_PANEL_TITLE, cfModel.getElementContent(CF_FAQ_QUESTION+ NUMBERS.get(faqCount.get(0))));
                ResourceUtil.getOrCreateResource(resourceResolver, resource.getPath()+CF_FAQ_ITEMS+faqCount.get(0), faqProperties, StringUtils.EMPTY, false);
                faqCount.remove(0);
            } catch (PersistenceException e) {
                LOG.error("Unable to persist the resource {}", e.getMessage());
            }
        }
    }

    public static boolean isPublish(SlingSettingsService slingSettings) {
        return slingSettings.getRunModes().contains(PUBLISH_ENV);
    }
}
