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
package org.sola.rules;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.drools.KnowledgeBase;
import org.drools.definition.type.FactType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    @Ignore
    public void testApp() {

        try {
            Example(false);
            Example(false);
            Example(false);
            RulesEngine.ClearCache();
            Example(false);
            Example(false);
            assertTrue(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void Example(Boolean test) throws Exception {
        RulesEngine engine = new RulesEngine();
        KnowledgeBase kbase = engine.LoadRules("SOLA");
        List<Object> facts = new ArrayList<Object>();

        FactType partyType = kbase.getFactType("SOLA", "Party");
        Object par1 = partyType.newInstance();
        partyType.set(par1, "id", "par1");
        Object par2 = partyType.newInstance();
        partyType.set(par2, "id", "par2");

        FactType appType = kbase.getFactType("SOLA", "Application");
        Object app = appType.newInstance();
        appType.set(app, "applicant", par1);
        if (!test) {
            appType.set(app, "contact", par2);
        }

        FactType targetType = kbase.getFactType("SOLA", "RuleTarget");
        Object tar = targetType.newInstance();
        targetType.set(tar, "application", app);

        facts.add(tar);
        engine.ExecuteRules(kbase, facts, null);
        Boolean out = (Boolean) targetType.get(tar, "outcome");
        String msg = "";
        if (out) {
            msg = "Result=" + out + ", Date=" + appType.get(app, "lodgedDatetime").toString();
        } else {
            msg = "Result=" + out + ", Msg=" + targetType.get(tar, "message").toString();
        }

        System.out.println(msg);
    }
}
