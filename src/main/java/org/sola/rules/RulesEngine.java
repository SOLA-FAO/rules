/**
 * ******************************************************************************************
 * Copyright (C) 2014 - Food and Agriculture Organization of the United Nations (FAO).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,this list
 *       of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice,this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *    3. Neither the name of FAO nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sola.rules;

import java.util.Calendar;
import java.util.Map;
import java.util.ResourceBundle;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.io.impl.UrlResource;
import org.drools.runtime.StatelessKnowledgeSession;

/**
 *
 * @author soladev
 */
public class RulesEngine extends AbstractRulesEngine {

    private static final String ENABLED = "ENABLED";
    private static final String CACHE_NAME = "org.sola.rules.cache";
    private String Version = "TEST"; // The version of the rule package to use (e.g. DEV, TEST, PROD, etc)
    private String BaseURL = "http://localhost:8180/drools-guvnor/org.drools.guvnor.Guvnor/package/"; // Base Drools Guvnor URL
    private String UserName = "admin"; // Username to authenticate with Guvnor
    private String Password = "admin"; // Password to authenticate with Guvnor
    private String Enabled = ENABLED; // Flag to indicate authentication with Guvnor is required

    public RulesEngine() {

        // Set the external properties for the RulesEngine
        ResourceBundle bundle = ResourceBundle.getBundle("Rules");
        if (bundle != null) {
            BaseURL = readProperty(bundle, BaseURL, "BASE_URL");
            Version = readProperty(bundle, Version, "PACKAGE_VERSION");
            Enabled = readProperty(bundle, Enabled, "AUTHENTICATION");
            UserName = readProperty(bundle, UserName, "USER");
            Password = readProperty(bundle, Password, "PASSWORD");
        } else {
            // TODO use info logging instead of println
            System.out.println("Properties file Rules does not exist in resources directory");
        }

    }

    public static void ClearCache() {
        CacheManager cacheMan = CacheManager.getInstance();
        if (cacheMan.cacheExists(CACHE_NAME)) {
            int size = cacheMan.getCache(CACHE_NAME).getKeysWithExpiryCheck().size();

            //TODO use logging instead of println
            System.out.println("Clearing " + size + " elements from cache " + CACHE_NAME);
            cacheMan.getCache(CACHE_NAME).removeAll();
        }
    }

    public void ExecuteRules(KnowledgeBase kbase, Iterable facts, Map<String, Object> globals) {
        StatelessKnowledgeSession session = kbase.newStatelessKnowledgeSession();
        if (globals != null) {
            // Add the list of globals to the session
            for (Map.Entry<String, Object> global : globals.entrySet()) {
                session.setGlobal(global.getKey(), global.getValue());
            }
        }
        // Execute the session
        session.execute(facts);
    }

    public KnowledgeBase LoadRules(String packageName) {

        String rulePackageURL = null;
        String cacheKey = packageName + "." + Version;

        CacheManager cacheMan = CacheManager.getInstance();
        Cache ruleCache = null;
        KnowledgeBase kBase = null;

        // Get a handle to the rules cache or create one if necessary with some
        // initial configuration. Note that the ttl and enternal settings
        // are customized as each Element is added to the cache so that
        // all Elements expire at midnight each day.
        if (!cacheMan.cacheExists(CACHE_NAME)) {
            ruleCache = new Cache(CACHE_NAME, 0, false, true, 30, 30);
            cacheMan.addCache(ruleCache);
        } else {
            ruleCache = cacheMan.getCache(CACHE_NAME);
        }

        Element ele = ruleCache.get(cacheKey);
        if (ele == null) {
            try {
                // TODO use logging instead of println
                System.out.println("Loading package " + cacheKey + " into " + CACHE_NAME);

                // Create a URLResource object pointing to the rule package location
                // in Guvnor
                rulePackageURL = BaseURL + packageName + "/" + Version;
                UrlResource resource = (UrlResource) ResourceFactory.newUrlResource(rulePackageURL);
                if (Enabled.equalsIgnoreCase(ENABLED)) {
                    resource.setBasicAuthentication(ENABLED);
                    resource.setUsername(UserName);
                    resource.setPassword(Password);
                }

                // Use a KnowledgeBuilder to download the rule package from Guvnor
                // and check for errors
                KnowledgeBuilder kBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
                kBuilder.add(resource, ResourceType.PKG);

                if (kBuilder.hasErrors()) {
                    for (KnowledgeBuilderError err : kBuilder.getErrors()) {
                        // TODO: update so that the errors are managed correcly
                        System.err.println(err.toString());
                    }
                    throw new IllegalStateException("PKG errors");
                }

                // Create the KnowledgeBase using the compiled KnowledgePackages
                // from the KnowledgeBuilder
                kBase = KnowledgeBaseFactory.newKnowledgeBase();
                kBase.addKnowledgePackages(kBuilder.getKnowledgePackages());

                // Cache the KnowledgeBase but set it to expire at midnight
                // so that any changes to the rule will be obtained for
                // the next working day.
                Element cacheElement = new Element(cacheKey, kBase);
                cacheElement.setEternal(false);
                Calendar currentTime = Calendar.getInstance();
                int ttl = 60 - currentTime.get(Calendar.SECOND);
                ttl += 60 * (59 - currentTime.get(Calendar.MINUTE));
                ttl += 3600 * (23 - currentTime.get(Calendar.HOUR_OF_DAY));
                cacheElement.setTimeToLive(ttl);
                ruleCache.put(cacheElement);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            // Get the KnowledgeBase from the cache
            kBase = (KnowledgeBase) ele.getValue();
        }

        return kBase;
    }

    private String readProperty(ResourceBundle bundle, String property, String propertyName) {
        if (bundle != null) {
            String tempProp = bundle.getString(propertyName);
            if (tempProp != null && !tempProp.isEmpty()) {
                property = tempProp;
            }
        }
        return property;
    }
}
