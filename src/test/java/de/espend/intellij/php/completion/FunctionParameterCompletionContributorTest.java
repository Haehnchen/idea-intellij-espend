package de.espend.intellij.php.completion;

import de.espend.intellij.ProjectPlatformTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FunctionParameterCompletionContributorTest extends ProjectPlatformTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("FunctionParameterCompletionContributor.php");
    }

    public String getTestDataPath() {
        return "src/test/java/de/espend/intellij/php/completion/fixtures";
    }

    public void testForFirstParameter() {
        assertCompletionContains(
            "test.php",
            createFunctionScope("foobar($<caret>);"),
            "$test->createFoobar()",
            "$test->fooProperty",
            "$this->getMyFoobar()"
        );

        assertCompletionNotContains(
            "test.php",
            createFunctionScope("foobar('test', $<caret>);"),
            "$test->createFoobar()",
            "$test->fooProperty",
            "$this->getMyFoobar()"
        );
    }

    public void testForLaterParameter() {
        assertCompletionContains(
            "test.php",
            createFunctionScope("foobar('test', 'test2', $<caret>);"),
            "$test->createFoobar()",
            "$test->fooProperty",
            "$this->getMyFoobar()"
        );
    }

    private String createFunctionScope(@NotNull String caretScope) {
        return "<?php\n" +
            "namespace Foobar;\n" +
            "\n" +
            "use App\\Foobar;\n" +
            "\n" +
            "class MyClass\n" +
            "{\n" +
            "    private function getMyFoobar(): \\App\\Foobar\n" +
            "    {\n" +
            "    }\n" +
            "    public function getFoobar(): void\n" +
            "    {\n" +
            "        \n" +
            "        $test = new \\App\\FoobarFactory();\n" +
            "        foobar(" + caretScope + ");" +
            "        \n" +
            "    }\n" +
            "}";
    }
}
