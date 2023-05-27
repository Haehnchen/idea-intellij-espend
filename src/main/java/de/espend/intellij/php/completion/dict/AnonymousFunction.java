package de.espend.intellij.php.completion.dict;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record AnonymousFunction (
    @NotNull String type,
    @NotNull String parameter,
    @NotNull String arrayParameter,
    @NotNull FunctionTyp functionTyp,
    boolean isStatic,
    @Nullable String reference,
    @Nullable ReferenceType referenceType,
    @Nullable String parameterArrayReference
) {
    public AnonymousFunction(@NotNull AnonymousFunctionMatch match, @NotNull String parameter, @NotNull FunctionTyp functionTyp, boolean isStatic) {
        this(match.type(), parameter, match.parameterArray(), functionTyp, isStatic, match.reference(), match.referenceType(), match.parameterArrayReference());
    }

    public enum ReferenceType {
        FIELD,
        METHOD
    }

    public enum FunctionTyp {
        ANONYMOUS,
        ARROW
    }

    public boolean isTypedClass() {
        return type.startsWith("\\");
    }

    public AnonymousFunction withTypeNewHint(@NotNull String string) {
        return new AnonymousFunction(string, parameter, arrayParameter, functionTyp, isStatic, reference, referenceType, parameterArrayReference);
    }

    public String toFunction() {
        String ref = createReferenceCall();
        String staticPrefix = this.isStatic ? "static " : "";

        String arrayParameter1 = arrayParameter;
        if (parameterArrayReference != null) {
            arrayParameter1 = parameterArrayReference;
        }

        return switch (functionTyp) {
            case ARROW -> staticPrefix + "fn(" + type +" $" + parameter + ") => $" + parameter + ref + ", $" + arrayParameter1;
            case ANONYMOUS -> staticPrefix + "function (" + type +" $" + parameter + ") { return $" + parameter + ref + ";}, $" + arrayParameter1;
        };
    }

    public String toLookupElementString() {
        String s = type;

        if (s.contains("\\")) {
            s = StringUtils.substringAfterLast(type, "\\");
        }

        String ref = createReferenceCall();
        String staticPrefix = this.isStatic ? "static " : "";

        String arrayParameter1 = arrayParameter;
        if (parameterArrayReference != null) {
            arrayParameter1 = parameterArrayReference;
        }

        return switch (functionTyp) {
            case ARROW -> staticPrefix + "fn(" + s +" $" + parameter + ") => $" + parameter + ref + ", $" + arrayParameter1;
            case ANONYMOUS -> staticPrefix + "function(" + s +" $" + parameter + ") => { return $" + parameter + ref + ";}, $" + arrayParameter1;
        };
    }

    private String createReferenceCall() {
        String ref = "";

        if (reference != null) {
            ref += "->";
            ref += reference;

            if (referenceType == ReferenceType.METHOD) {
                ref += "()";
            }
        }

        return ref;
    }
}