package de.espend.intellij;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class DummyTest extends BasePlatformTestCase {
    public void testDummy() {
        assertTrue(new Dummy().fun());
    }
}
