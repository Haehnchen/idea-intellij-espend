package de.espend.intellij.php.completion;

import de.espend.intellij.ProjectPlatformTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ForParameterCompletionContributorTest extends ProjectPlatformTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/de/espend/intellij/php/completion/fixtures";
    }

    public void testThatArrayMapWithParameterScopeProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            createForeachContent("fore<caret>"),
            "foreach ($test->cars as $car) {}",
            "foreach ($test->myIds as $myId) {}",
            "foreach ($test->getFoobar() as $foobar) {}",
            "foreach ($prices as $price) {}",
            "foreach ($items as $item) {}",
            "foreach ($ids as $id) {}"
        );
    }

    private String createForeachContent(@NotNull String caretScope) {
        return "<?php\n" +
            "namespace Foobar;\n" +
            "\n" +
            "use App\\Foobar;\n" +
            "\n" +
            "class MyClass\n" +
            "{\n" +
            "    public function getFoobar(): void\n" +
            "    {\n" +
            "        $items = ['test', 'test2'];\n" +
            "        $ids = [12, 12];\n" +
            "        $prices = [12.12, 12.12];\n" +
            "        \n" +
            "        $test = new Foobar();\n" +
            "        " + caretScope + ";" +
            "        \n" +
            "    }\n" +
            "}";
    }
}
