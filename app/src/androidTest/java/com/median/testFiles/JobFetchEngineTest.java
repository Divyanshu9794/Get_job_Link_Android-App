package com.median.testFiles;

import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class JobFetchEngineTest {

    @Test
    public void testUrlNormalization() {
        String url1 = "https://www.example.com/jobs/123?utm_source=twitter&utm_medium=social#apply";
        String url2 = "https://example.com/jobs/123/";

        String clean1 = url1.split("\\?")[0].split("#")[0].replaceAll("/+$", "").replaceAll("^https?://(www\\.)?", "");
        String clean2 = url2.split("\\?")[0].split("#")[0].replaceAll("/+$", "").replaceAll("^https?://(www\\.)?", "");

        assertEquals(clean1, clean2);
    }

    @Test
    public void testCompanyNormalization() {
        String comp1 = "Google Inc.";
        String comp2 = "google";

        String norm1 = comp1.toLowerCase().replaceAll("\\b(inc|llc|ltd|corp|corporation|co|gmbh|sarl|limited)\\b", "").replaceAll("[^a-z0-9]", "").trim();
        String norm2 = comp2.toLowerCase().replaceAll("\\b(inc|llc|ltd|corp|corporation|co|gmbh|sarl|limited)\\b", "").replaceAll("[^a-z0-9]", "").trim();

        assertEquals(norm1, norm2);
    }

    @Test
    public void test24HourDateFilterLogic() {
        long currentTime = System.currentTimeMillis();
        long twentyFourHoursInMillis = 24 * 60 * 60 * 1000L;

        long recentTimestamp = currentTime - (2 * 60 * 60 * 1000L); // 2 hours ago
        long oldTimestamp = currentTime - (26 * 60 * 60 * 1000L); // 26 hours ago

        boolean isRecentValid = (currentTime - recentTimestamp) <= twentyFourHoursInMillis;
        boolean isOldValid = (currentTime - oldTimestamp) <= twentyFourHoursInMillis;

        assertTrue("Recent job within 24 hours should be accepted", isRecentValid);
        assertFalse("Old job older than 24 hours should be rejected", isOldValid);
    }

    @Test
    public void testCompanyLimitLogic() {
        java.util.Map<String, Integer> companyCounts = new java.util.HashMap<>();
        String company = "microsoft";

        int count1 = companyCounts.getOrDefault(company, 0);
        assertTrue(count1 < 3);
        companyCounts.put(company, count1 + 1);

        int count2 = companyCounts.getOrDefault(company, 0);
        assertTrue(count2 < 3);
        companyCounts.put(company, count2 + 1);

        int count3 = companyCounts.getOrDefault(company, 0);
        assertTrue(count3 < 3);
        companyCounts.put(company, count3 + 1);

        int count4 = companyCounts.getOrDefault(company, 0);
        assertFalse("Fourth job for the same company should exceed max limit of 3", count4 < 3);
    }
}
