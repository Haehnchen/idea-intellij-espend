package de.espend.intellij.php.completion.lookupElement;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.PlatformIcons;
import com.jetbrains.php.PhpIcons;
import de.espend.intellij.php.completion.dict.AnonymousFunction;
import de.espend.intellij.php.completion.insertHandler.AnonymousFunctionInsertHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnonymousFunctionLookupElement extends LookupElement {
    private final AnonymousFunction anonymousFunction;

    public AnonymousFunctionLookupElement(@NotNull AnonymousFunction anonymousFunction) {
        this.anonymousFunction = anonymousFunction;
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        if (anonymousFunction.referenceType() == AnonymousFunction.ReferenceType.METHOD) {
            presentation.setIcon(PhpIcons.METHOD);
        } else if (anonymousFunction.referenceType() == AnonymousFunction.ReferenceType.FIELD) {
            presentation.setIcon(PhpIcons.FIELD);
        } else {
            presentation.setIcon(PlatformIcons.FUNCTION_ICON);
        }

        super.renderElement(presentation);
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        new AnonymousFunctionInsertHandler().handleInsert(context, this);
    }

    @NotNull
    public AnonymousFunction getAnonymousFunction() {
        return anonymousFunction;
    }

    @Override
    public @NotNull String getLookupString() {
        return anonymousFunction.toLookupElementString();
    }
}