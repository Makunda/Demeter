/*
 * Copyright (C) 2020  Hugo JOBY
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License v3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public v3
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.demeter.tags;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.exceptions.neo4j.Neo4JTemplateLanguageException;

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

    public TagProcessing() {
    }

    /**
     * Replace the Context Anchor by the name of the application
     *
     * @param request
     * @param context Label of the application
     * @return
     */
    public static String processApplicationContext(String request, String context) {
        return request.replace(LABEL_ANCHOR, context);
    }

    /**
     * Replace the application context anchor by a dummy application name. (TEST PURPOSES)
     *
     * @param request Request to treat
     * @return
     */
    public static String replaceDummyApplicationContext(String request) {
        return request.replace(LABEL_ANCHOR, "DummyApplication");
    }

    /**
     * Replace the Tag setting anchor
     *
     * @param request
     * @return
     */
    public static String processTagSet(String request) throws Neo4JTemplateLanguageException {
        Pattern p = Pattern.compile(ANCHOR_TAG_SET);
        Matcher m = p.matcher(request);

        if (m.find()) {
            if (m.groupCount() < 1)
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
     *
     * @param request
     * @return
     */
    public static String processReturn(String request) throws Neo4JTemplateLanguageException {
        Pattern p = Pattern.compile(ANCHOR_RETURN);
        Matcher m = p.matcher(request);

        if (m.find()) {
            // find variable name
            if (m.groupCount() < 1)
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
     *
     * @param request
     * @return
     */
    public static String forgeCountRequest(String request) throws Neo4JTemplateLanguageException {
        Pattern p = Pattern.compile(ANCHOR_RETURN);
        Matcher m = p.matcher(request);

        if (m.find()) {
            // find variable name
            if (m.groupCount() < 1)
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
     * Check the presence of a Count anchor request
     *
     * @param request The request to test
     * @return True if the anchor was detected in the request
     */
    public static boolean isCountAnchorPresent(String request) {
        Pattern p = Pattern.compile(ANCHOR_RETURN);
        Matcher m = p.matcher(request);
        return m.find();
    }

    /**
     * Check the presence of a return anchor request
     *
     * @param request The request to test
     * @return True if the anchor was detected in the request
     */
    public static boolean isReturnAnchorPresent(String request) {
        Pattern p = Pattern.compile(ANCHOR_RETURN);
        Matcher m = p.matcher(request);
        return m.find();
    }

    /**
     * Remove anchors still present in the code
     *
     * @param request The request to "Sanitize" from anchors
     * @return <code>String</code> Request cleaned
     */
    public static String removeRemainingAnchors(String request) {
        for (String anchor : ANCHOR_LIST) {
            request = request.replaceAll(anchor, "");
        }
        return request;
    }

    /**
     * Replace All Anchors in the code;
     *
     * @param request
     * @return
     */
    public static String processAll(String request) throws Neo4JTemplateLanguageException {
        request = processReturn(request);
        return processTagSet(request);
    }
}
