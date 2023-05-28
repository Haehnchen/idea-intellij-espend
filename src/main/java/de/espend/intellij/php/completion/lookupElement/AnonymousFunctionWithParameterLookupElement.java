package de.espend.intellij.php.completion.lookupElement;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.PlatformIcons;
import com.jetbrains.php.PhpIcons;
import de.espend.intellij.php.completion.dict.AnonymousFunctionWithParameter;
import de.espend.intellij.php.completion.insertHandler.AnonymousFunctionInsertHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnonymousFunctionWithParameterLookupElement extends LookupElement {
    private final AnonymousFunctionWithParameter anonymousFunctionWithParameter;

    public AnonymousFunctionWithParameterLookupElement(@NotNull AnonymousFunctionWithParameter anonymousFunctionWithParameter) {
        this.anonymousFunctionWithParameter = anonymousFunctionWithParameter;
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        if (anonymousFunctionWithParameter.referenceType() == AnonymousFunctionWithParameter.ReferenceType.METHOD) {
            presentation.setIcon(PhpIcons.METHOD);
        } else if (anonymousFunctionWithParameter.referenceType() == AnonymousFunctionWithParameter.ReferenceType.FIELD) {
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
    public AnonymousFunctionWithParameter getAnonymousFunction() {
        return anonymousFunctionWithParameter;
    }

    @Override
    public @NotNull String getLookupString() {
        return anonymousFunctionWithParameter.toLookupElementString();
    }
}