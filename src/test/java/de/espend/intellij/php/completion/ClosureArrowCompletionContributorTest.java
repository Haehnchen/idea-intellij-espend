package de.espend.intellij.php.completion;

import de.espend.intellij.ProjectPlatformTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClosureArrowCompletionContributorTest extends ProjectPlatformTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public void testThatArrayMapWithParameterScopeProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            createArrayMapContent("<caret> , $ids"),
            "fn(int $id) => $id"
        );
    }

    public void testThatArrayMapWithTypedParameterScopeProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            createArrayMapContent("<caret> , $test->cars"),
            "fn(Foobar $foobar) => $foobar",
            "fn(string $car) => $car->myIds"
        );
    }

    public void testThatArrayMapProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            createArrayMapContent("<caret>"),
            "fn(float $price) => $price, $prices",
            "fn(int $id) => $id, $ids"
        );
    }

    public void testThatArrayMapForArrayProvidesCompletion() {
        assertCompletionContains("test.php",
            createArrayMapContent("fn<caret>"),
            "fn(float $price) => $price, $prices",
            "fn(int $id) => $id, $ids",
            "fn(string $item) => $item, $items",
            "fn(Foobar $foobar) => $foobar, $test->getFoobar()",
            "fn(Foobar $foobar) => $foobar, $test->cars",
            "fn(float $price) => $price, $prices"
        );
    }

    public void testThatArrayMapForAnonymousProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            createArrayMapContent("func<caret>"),
            "function(float $price) => { return $price;}, $prices",
            "function(Foobar $foobar) => { return $foobar;}, $test->cars",
            "function(Foobar $foobar) => { return $foobar;}, $test->getFoobar()",
            "function(int $id) => { return $id;}, $ids",
            "function(string $item) => { return $item;}, $items"
        );
    }

    public void testThatArrayMapWithStaticProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            createArrayMapContent("st<caret>"),
            "static fn(float $price) => $price, $prices",
            "static fn(int $id) => $id, $ids",
            "static fn(string $item) => $item, $items",
            "static fn(Foobar $foobar) => $foobar, $test->getFoobar()",
            "static fn(Foobar $foobar) => $foobar, $test->cars"
        );
    }
    public String getTestDataPath() {
        return "src/test/java/de/espend/intellij/php/completion/fixtures";
    }

    private String createArrayMapContent(@NotNull String caretScope) {
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
            "        array_map(" + caretScope + ");" +
            "        \n" +
            "    }\n" +
            "}";
    }
}
