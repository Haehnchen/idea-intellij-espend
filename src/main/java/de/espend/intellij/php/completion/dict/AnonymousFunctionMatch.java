package de.espend.intellij.php.completion.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record AnonymousFunctionMatch (
    @NotNull String parameterArray,
    @NotNull String type,
    int weight,
    @Nullable String reference,
    @Nullable AnonymousFunction.ReferenceType referenceType,

    @Nullable String parameterArrayReference
) {
    public AnonymousFunctionMatch(String parameterArray, String type, int weight) {
        this(parameterArray, type, weight, null, null, null);
    }
}