package de.espend.intellij.php.completion.dict;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

public class AnonymousFunction extends AnonymousFunctionWithParameter {
    public AnonymousFunction(@NotNull AnonymousFunctionMatch match, @NotNull String parameter, @NotNull FunctionTyp functionTyp, boolean isStatic) {
        super(match, parameter, functionTyp, isStatic);
    }

    @Override
    public String toFunction() {
        String ref = createReferenceCall();
        String staticPrefix = this.isStatic() ? "static " : "";

        return switch (functionTyp()) {
            case ARROW -> staticPrefix + "fn(" + type() +" $" + parameter() + ") => $" + parameter() + ref;
            case ANONYMOUS -> staticPrefix + "function (" + type() +" $" + parameter() + ") { return $" + parameter() + ref + ";}";
        };
    }

    @Override
    public String toLookupElementString() {
        String s = type();

        if (s.contains("\\")) {
            s = StringUtils.substringAfterLast(type(), "\\");
        }

        String ref = createReferenceCall();
        String staticPrefix = this.isStatic() ? "static " : "";

        return switch (functionTyp()) {
            case ARROW -> staticPrefix + "fn(" + s + " $" + parameter() + ") => $" + parameter() + ref;
            case ANONYMOUS -> staticPrefix + "function(" + s + " $" + parameter() + ") => { return $" + parameter() + ref + ";})";
        };
    }
}
