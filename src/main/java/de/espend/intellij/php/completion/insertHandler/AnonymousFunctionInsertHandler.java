package de.espend.intellij.php.completion.insertHandler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import de.espend.intellij.php.PhpElementsUtil;
import de.espend.intellij.php.completion.dict.AnonymousFunctionWithParameter;
import de.espend.intellij.php.completion.lookupElement.AnonymousFunctionLookupElement;
import de.espend.intellij.php.completion.lookupElement.AnonymousFunctionWithParameterLookupElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnonymousFunctionInsertHandler implements InsertHandler<LookupElement> {
    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
        context.commitDocument();

        if (item instanceof AnonymousFunctionWithParameterLookupElement element) {
            withRightParameterInsert(context, element);
        } else if (item instanceof AnonymousFunctionLookupElement element) {
            insert(context, element);
        }
    }

    private static void withRightParameterInsert(@NotNull InsertionContext context, AnonymousFunctionWithParameterLookupElement item) {
        AnonymousFunctionWithParameter anonymousFunctionWithParameter = item.getAnonymousFunction();
        PsiElement elementAt = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
        if (elementAt == null) {
            return;
        }

        if (anonymousFunctionWithParameter.isTypedClass()) {
            String string = PhpElementsUtil.insertUseIfNecessary(elementAt, anonymousFunctionWithParameter.type());
            if (string != null) {
                anonymousFunctionWithParameter = anonymousFunctionWithParameter.withTypeNewHint(string);
            }
        }

        PsiElement functionText = PhpPsiElementFactory.createFunctionReference(context.getProject(), "f(" + anonymousFunctionWithParameter.toFunction() +");").getParameterList();

        FunctionReference parentOfType = PsiTreeUtil.getParentOfType(elementAt, FunctionReference.class);
        parentOfType.addAfter(functionText, parentOfType.getFirstChild().getNextSibling());

        int caretOffset = context.getEditor().getCaretModel().getOffset();

        int caretPosition;

        if (anonymousFunctionWithParameter.isLeftSide()) {
            caretPosition = functionText.getText().length();
        } else {
            caretPosition = anonymousFunctionWithParameter.functionTyp() == AnonymousFunctionWithParameter.FunctionTyp.ARROW
                ? functionText.getText().indexOf(",")
                : functionText.getText().indexOf(";");
        }

        context.getEditor().getCaretModel().moveToOffset(caretOffset + caretPosition);
    }

    private static void insert(@NotNull InsertionContext context, AnonymousFunctionLookupElement item) {
        AnonymousFunctionWithParameter anonymousFunctionWithParameter = item.getAnonymousFunction();
        PsiElement elementAt = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
        if (elementAt == null) {
            return;
        }

        if (anonymousFunctionWithParameter.isTypedClass()) {
            String string = PhpElementsUtil.insertUseIfNecessary(elementAt, anonymousFunctionWithParameter.type());
            if (string != null) {
                anonymousFunctionWithParameter = anonymousFunctionWithParameter.withTypeNewHint(string);
            }
        }

        PsiElement functionText = PhpPsiElementFactory.createFunctionReference(context.getProject(), "f(" + anonymousFunctionWithParameter.toFunction() +");").getParameterList();
        FunctionReference parentOfType = PsiTreeUtil.getParentOfType(elementAt, FunctionReference.class);
        parentOfType.addAfter(functionText, elementAt.getPrevSibling());

        int caretOffset = context.getEditor().getCaretModel().getOffset();

        context.getEditor().getCaretModel().moveToOffset(caretOffset + functionText.getText().length());
    }
}
