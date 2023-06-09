package de.espend.intellij.php.completion.dict;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnonymousFunctionWithParameter {
    private final @NotNull String type;
    private final @NotNull String parameter;
    private final @NotNull String arrayParameter;

    private final @NotNull FunctionTyp functionTyp;
    private final boolean isStatic;
    private final @Nullable String reference;
    private final @Nullable ReferenceType referenceType;
    private final @Nullable String parameterArrayReference;

    private boolean leftSide = false;

    public AnonymousFunctionWithParameter(
        @NotNull String type,
        @NotNull String parameter,
        @NotNull String arrayParameter,
        @NotNull FunctionTyp functionTyp,
        boolean isStatic,
        @Nullable String reference,
        @Nullable ReferenceType referenceType,
        @Nullable String parameterArrayReference,
        boolean leftSide
    ) {
        this.type = type;
        this.parameter = parameter;
        this.arrayParameter = arrayParameter;
        this.functionTyp = functionTyp;
        this.isStatic = isStatic;
        this.reference = reference;
        this.referenceType = referenceType;
        this.parameterArrayReference = parameterArrayReference;
        this.leftSide = leftSide;
    }

    public AnonymousFunctionWithParameter(@NotNull AnonymousFunctionMatch match, @NotNull String parameter, @NotNull FunctionTyp functionTyp, boolean isStatic) {
        this(match.type(), parameter, match.parameterArray(), functionTyp, isStatic, match.reference(), match.referenceType(), match.parameterArrayReference(), false);
    }

    public AnonymousFunctionWithParameter(@NotNull AnonymousFunctionMatch match, @NotNull String parameter, @NotNull FunctionTyp functionTyp, boolean isStatic, boolean leftSide) {
        this(match.type(), parameter, match.parameterArray(), functionTyp, isStatic, match.reference(), match.referenceType(), match.parameterArrayReference(), leftSide);
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

    public AnonymousFunctionWithParameter withTypeNewHint(@NotNull String string) {
        return new AnonymousFunctionWithParameter(string, parameter, arrayParameter, functionTyp, isStatic, reference, referenceType, parameterArrayReference, leftSide);
    }

    public String toFunction() {
        String ref = createReferenceCall();
        String staticPrefix = this.isStatic ? "static " : "";

        String arrayParameter1 = arrayParameter;
        if (parameterArrayReference != null) {
            arrayParameter1 = parameterArrayReference;
        }

        if (leftSide) {
            return switch (functionTyp) {
                case ARROW -> "$" + arrayParameter1 + ", " + staticPrefix + "fn(" + type + " $" + parameter + ") => $" + parameter + ref;
                case ANONYMOUS -> "$" + arrayParameter1 + ", " + staticPrefix + "function (" + type + " $" + parameter + ") { return $" + parameter + ref + ";}";
            };
        }

        return switch (functionTyp) {
            case ARROW -> staticPrefix + "fn(" + type + " $" + parameter + ") => $" + parameter + ref + ", $" + arrayParameter1;
            case ANONYMOUS -> staticPrefix + "function (" + type + " $" + parameter + ") { return $" + parameter + ref + ";}, $" + arrayParameter1;
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

        if (leftSide) {
            return switch (functionTyp) {
                case ARROW -> "$" + arrayParameter1 + ", " + "fn(" + s + " $" + parameter + ") => $" + parameter + ref;
                case ANONYMOUS -> "$" + arrayParameter1 + ", " + "function(" + s + " $" + parameter + ") => { return $" + parameter + ref + ";}";
            };
        }

        return switch (functionTyp) {
            case ARROW -> staticPrefix + "fn(" + s + " $" + parameter + ") => $" + parameter + ref + ", $" + arrayParameter1;
            case ANONYMOUS -> staticPrefix + "function(" + s + " $" + parameter + ") => { return $" + parameter + ref + ";}, $" + arrayParameter1;
        };
    }

    public String createReferenceCall() {
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

    public @NotNull String type() {
        return type;
    }

    public @NotNull String parameter() {
        return parameter;
    }

    public @NotNull String arrayParameter() {
        return arrayParameter;
    }

    public @NotNull FunctionTyp functionTyp() {
        return functionTyp;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public @Nullable String reference() {
        return reference;
    }

    public @Nullable ReferenceType referenceType() {
        return referenceType;
    }

    public @Nullable String parameterArrayReference() {
        return parameterArrayReference;
    }

    public boolean isLeftSide() {
        return leftSide;
    }
}