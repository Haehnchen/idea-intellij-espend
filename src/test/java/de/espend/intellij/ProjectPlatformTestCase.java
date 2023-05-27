package de.espend.intellij;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class ProjectPlatformTestCase extends BasePlatformTestCase {
    public void assertCompletionContains(String filename, String configureByText, String... lookupStrings) {
        if (lookupStrings.length == 0) {
            throw new RuntimeException("no lookup element provided");
        }

        myFixture.configureByText(filename, configureByText);
        myFixture.completeBasic();

        List<String> lookupElementStrings = myFixture.getLookupElementStrings();
        if (lookupElementStrings == null) {
            lookupElementStrings = new ArrayList<>();
        }

        assertContainsElements(lookupElementStrings, lookupStrings);
    }

    public void assertCompletionNotContains(String filename, String configureByText, String... lookupStrings) {
        if (lookupStrings.length == 0) {
            throw new RuntimeException("no lookup element provided");
        }

        myFixture.configureByText(filename, configureByText);
        myFixture.completeBasic();

        List<String> lookupElementStrings = myFixture.getLookupElementStrings();
        if (lookupElementStrings == null) {
            lookupElementStrings = new ArrayList<>();
        }

        assertDoesntContain(lookupElementStrings, lookupStrings);
    }
}
