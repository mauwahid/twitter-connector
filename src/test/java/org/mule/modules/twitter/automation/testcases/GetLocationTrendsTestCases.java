/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is
 * published under the terms of the CPAL v1.0 license, a copy of which
 * has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.twitter.automation.testcases;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.modules.tests.ConnectorTestUtils;
import org.mule.modules.twitter.automation.RegressionTests;
import org.mule.modules.twitter.automation.TwitterTestParent;
import twitter4j.Trends;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GetLocationTrendsTestCases extends TwitterTestParent {

    @Category({RegressionTests.class})
    @Test
    public void testGetLocationTrendsDefaultValues() {
        try {
            Trends trends = runFlowAndGetPayload("get-location-trends-default-values");
            assertNotNull(trends);

        } catch (Exception e) {
            fail(ConnectorTestUtils.getStackTrace(e));
        }

    }

    @Category({RegressionTests.class})
    @Test
    public void testGetLocationTrendsParameterized() {
        initializeTestRunMessage("placeByWOEID");
        try {
            Trends trends = runFlowAndGetPayload("get-location-trends-parametrized");
            assertNotNull(trends);

        } catch (Exception e) {
            fail(ConnectorTestUtils.getStackTrace(e));
        }

    }

}
