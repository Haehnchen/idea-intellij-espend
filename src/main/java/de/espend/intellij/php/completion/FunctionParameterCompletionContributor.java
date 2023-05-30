package de.espend.intellij.php.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import de.espend.intellij.php.PhpElementsUtil;
import de.espend.intellij.php.completion.utils.AnonymousFunctionUtil;
import de.espend.intellij.php.utils.ElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FunctionParameterCompletionContributor extends CompletionContributor {
    public FunctionParameterCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                PsiElement originalPosition = completionParameters.getOriginalPosition();
                if (originalPosition == null) {
                    return;
                }

                // foo($<caret>)
                if (originalPosition.getNode().getElementType() == PhpTokenTypes.DOLLAR && originalPosition.getParent() instanceof Variable variable && variable.getParent() instanceof ParameterList parameterList && parameterList.getParent() instanceof FunctionReference functionReference) {
                    visitParameter(result, originalPosition, variable, functionReference);
                }
            }
        });
    }

    private static void visitParameter(@NotNull CompletionResultSet result, @NotNull PsiElement originalPosition, @NotNull Variable variable, @NotNull FunctionReference functionReference) {
        int currentParameterIndex = PhpElementsUtil.getCurrentParameterIndex(functionReference.getParameters(), variable);
        if (currentParameterIndex < 0) {
            return;
        }

        if (functionReference.resolve() instanceof Function function) {
            Parameter parameter = function.getParameter(currentParameterIndex);
            if (parameter == null) {
                return;
            }

            PhpType type1 = parameter.getType();

            Project project = originalPosition.getProject();

            for (Map.Entry<String, PhpType> entry : ElementsUtil.collectVariablesWithTypes(originalPosition).entrySet()) {
                for (PhpClass phpClass : AnonymousFunctionUtil.getClassesFromTyp(project, entry.getValue())) {
                    boolean isThisScope = "this".equals(entry.getKey());

                    for (Method method : AnonymousFunctionUtil.getValidMethods(phpClass, isThisScope)) {
                        if (PhpIndex.getInstance(project).completeType(project, method.getType(), new HashSet<>()).isConvertibleFrom(project, type1)) {
                            result.addElement(LookupElementBuilder.create("$" + entry.getKey() + "->" + method.getName() + "()"));
                        }
                    }

                    for (Field field : AnonymousFunctionUtil.getValidFields(phpClass, isThisScope)) {
                        if (PhpIndex.getInstance(project).completeType(project, field.getType(), new HashSet<>()).isConvertibleFrom(project, type1)) {
                            result.addElement(LookupElementBuilder.create("$" + entry.getKey() + "->" + field.getName()));
                        }
                    }
                }
            }
        }
    }
}
