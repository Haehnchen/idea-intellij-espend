package de.espend.intellij.php.completion.insertHandler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import de.espend.intellij.php.PhpElementsUtil;
import de.espend.intellij.php.completion.dict.AnonymousFunction;
import de.espend.intellij.php.completion.lookupElement.AnonymousFunctionLookupElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnonymousFunctionInsertHandler implements InsertHandler<LookupElement> {
    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
        context.commitDocument();

        if (!(item instanceof AnonymousFunctionLookupElement)) {
            return;
        }

        AnonymousFunction anonymousFunction = ((AnonymousFunctionLookupElement) item).getAnonymousFunction();
        PsiElement elementAt = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
        if (elementAt == null) {
            return;
        }

        if (anonymousFunction.isTypedClass()) {
            String string = PhpElementsUtil.insertUseIfNecessary(elementAt, anonymousFunction.type());
            if (string != null) {
                anonymousFunction = anonymousFunction.withTypeNewHint(string);
            }
        }

        PsiElement functionText = PhpPsiElementFactory.createFunctionReference(context.getProject(), "f(" + anonymousFunction.toFunction() +");").getParameterList();

        FunctionReference parentOfType = PsiTreeUtil.getParentOfType(elementAt, FunctionReference.class);
        parentOfType.addAfter(functionText, parentOfType.getFirstChild().getNextSibling());

        int caretOffset = context.getEditor().getCaretModel().getOffset();

        int caretPosition = anonymousFunction.functionTyp() == AnonymousFunction.FunctionTyp.ARROW
            ? functionText.getText().indexOf(",")
            : functionText.getText().indexOf(";");

        context.getEditor().getCaretModel().moveToOffset(caretOffset + caretPosition);
    }
}
