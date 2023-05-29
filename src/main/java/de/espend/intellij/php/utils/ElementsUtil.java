package de.espend.intellij.php.utils;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElementsUtil {
    public static Map<String, PhpType> collectVariablesWithTypes(@NotNull PsiElement scope) {
        // see for more use cases:
        // com.jetbrains.php.lang.psi.elements.impl.VariableImpl.getVariableVariants(com.intellij.psi.PsiElement, com.intellij.psi.PsiElement, boolean)
        // Stream var10000 = PhpVariantsUtil.getVariableVariants(position, parameters.getOriginalPosition(), allGlobals, allSuperGlobals).stream().filter((e) -> {
        // PhpUndeclaredVariableCompletionProvider
        // PhpCompletionContributor.visitScopeHolderBranches(position, (element) -> {
        // PhpPsiUtil.getScopeHolder(position)

        Map<String, PhpType> vars = new HashMap<>();

        Project project = scope.getProject();

        LookupElement[] variableVariants = VariableImpl.getVariableVariants(scope, false);
        for (LookupElement variableVariant : variableVariants) {
            if (variableVariant instanceof PhpLookupElement phpLookupElement) {
                String lookupString = variableVariant.getLookupString();

                Set<String> types = new HashSet<>();
                PhpType phpType = phpLookupElement.getPhpType();

                if (phpType != null) {
                    types.addAll(phpType.getTypes());
                }

                PhpNamedElement namedElement = phpLookupElement.getNamedElement();
                if (namedElement != null) {
                    types.addAll(namedElement.getType().getTypes());
                }

                vars.putIfAbsent(lookupString, new PhpType());
                for (String type : types) {
                    vars.get(lookupString).add(type);
                }

                vars.put(lookupString, PhpIndex.getInstance(project).completeType(project, vars.get(lookupString), new HashSet<>()));
            }
        }

        return vars;
    }
}
