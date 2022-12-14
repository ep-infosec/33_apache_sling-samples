/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.graphql.samples.website.datafetchers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.apache.sling.graphql.samples.website.models.SlingWrappers;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retrieve additional information for our articles 'seeAlso' field */
@Component(service = SlingDataFetcher.class, property = {"name=website/seeAlso"})
public class SeeAlsoDataFetcher implements SlingDataFetcher<Object> {

    private static final Logger log = LoggerFactory.getLogger(SeeAlsoDataFetcher.class);

    /**
     * For "see also", our articles have just the article name but no path or
     * section. This maps those names (which are Sling Resource names) to their
     * Resource, so we can use the full path + title to render links.
     */
    private static Map<String, Object> toArticleRef(ResourceResolver resolver, String nodeName) {
        final String jcrQuery = String.format("/jcr:root%s//*[@filename='%s']", Constants.ARTICLES_ROOT, nodeName);
        final Iterator<Resource> it = resolver.findResources(jcrQuery, "xpath");

        // We want zero to one result: do not fail for dangling "see also" references,
        // just log them
        if (!it.hasNext()) {
            log.warn("No Resource Found: {}", jcrQuery);
            return Collections.emptyMap();
        }
        final Map<String, Object> result = SlingWrappers.resourceWrapper(it.next());
        if (it.hasNext()) {
            throw new RuntimeException("More than one Resource found:" + jcrQuery);
        }

        return result;
    }

    @Override
    public Object get(SlingDataFetcherEnvironment env) throws Exception {
        // Our "see also" field only contains node names - this demonstrates
        // using a query to get their full paths
        //
        final ValueMap vm = FetcherUtil.getSourceResource(env).adaptTo(ValueMap.class);
        if (vm != null) {
            return Arrays.stream(vm.get("seeAlso", String[].class))
                    .map(it -> toArticleRef(env.getCurrentResource().getResourceResolver(), it)).toArray();
        }
        return null;
    }
}