package com.castsoftware.tagging.tags;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.exceptions.neo4j.Neo4JTemplateLanguageException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagProcessing {
    // Properties
    public static final String ANCHOR_TAG_SET = Configuration.get("tag.anchors.tag_set");
    public static final String ANCHOR_TAG_SET_VALUE = Configuration.get("tag.anchors.tag_set.value");

    public static final String ANCHOR_RETURN = Configuration.get("tag.anchors.return");
    public static final String ANCHOR_RETURN_VALUE = Configuration.get("tag.anchors.return.label");

    public static final String ANCHOR_COUNT_RETURN_VALUE = Configuration.get("tag.anchors.countReturn.label");

    public static final String LABEL_ANCHOR = Configuration.get("tag.anchors.label");

    // Label anchor is not present in this list. It is the only mandatory label that request a replacement to allow the request to be functional
    public static final List<String> ANCHOR_LIST = Arrays.asList(ANCHOR_TAG_SET, ANCHOR_RETURN);

    /**
     * Replace the Context Anchor by the name of the application
     * @param request
     * @param context Label of the application
     * @return
     */
    public static String processApplicationContext(String request, String context) {
        return request.replace(LABEL_ANCHOR, context);
    }

    /**
     * Replace the Tag setting anchor
     * @param request
     * @return
     */
    public static String processTagSet(String request) throws Neo4JTemplateLanguageException {
        Pattern p = Pattern.compile(ANCHOR_TAG_SET);
        Matcher m = p.matcher(request);

        if(m.find()) {
            if(m.groupCount() < 1 )
                throw new Neo4JTemplateLanguageException("Invalid tag set usage.", request, "TAGPxPRTS01");

            // find variable name
            String o = m.group(1);

            // forge new value
            String replacer = ANCHOR_TAG_SET_VALUE.replace("@", o);

            // Modify original request;
            request = request.replaceFirst(ANCHOR_TAG_SET, replacer);
        }

        return request;
    }

    /**
     * Replace the return anchor
     * @param request
     * @return
     */
    public static String processReturn(String request) throws Neo4JTemplateLanguageException {
        Pattern p = Pattern.compile(ANCHOR_RETURN);
        Matcher m = p.matcher(request);

        if(m.find()) {
            // find variable name
            if(m.groupCount() < 1 )
                throw new Neo4JTemplateLanguageException("Invalid return tag usage.", request, "TAGPxPRRT01");

            String o = m.group(1);

            // Forge new value
            String replacer = ANCHOR_RETURN_VALUE.replace("@", o);

            // Modify original request;
            request = request.replaceFirst(ANCHOR_RETURN, replacer);
        }

        return request;
    }

    /**
     * Replace the RETURN anchor by a COUNT_RETURN anchor. Remove every other anchors encountered.
     * @param request
     * @return
     */
    public static String forgeCountRequest(String request) throws Neo4JTemplateLanguageException {
        Pattern p = Pattern.compile(ANCHOR_RETURN);
        Matcher m = p.matcher(request);

        if(m.find()) {
            // find variable name
            if(m.groupCount() < 1 )
                throw new Neo4JTemplateLanguageException("Invalid count tag usage.", request, "TAGPxFCOR01");

            String o = m.group(1);

            // Forge new value
            String replacer = ANCHOR_COUNT_RETURN_VALUE.replace("@", o);

            // Modify original request;
            request = request.replaceFirst(ANCHOR_RETURN, replacer);
        }

        return removeRemainingAnchors(request);
    }

    /**
     * Remove anchors still present in the code
     * @param request The request to "Sanitize" from anchors
     * @return <code>String</code> Request cleaned
     */
    public static String removeRemainingAnchors(String request) {
        for(String anchor : ANCHOR_LIST) {
            request = request.replaceAll(anchor, "");
        }
        return request;
    }

    /**
     * Replace All Anchors in the code;
     * @param request
     * @return
     */
    public static String processAll(String request) throws Neo4JTemplateLanguageException {
        request = processReturn(request);
        return processTagSet(request);
    }

    public TagProcessing() {
    }
}
