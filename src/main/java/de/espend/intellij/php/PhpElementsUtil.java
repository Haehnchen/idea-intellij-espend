package de.espend.intellij.php;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.refactoring.PhpAliasImporter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpElementsUtil {
    public static String insertUseIfNecessary(@NotNull PsiElement phpClass, @NotNull String fqnClasName) {
        if(!fqnClasName.startsWith("\\")) {
            fqnClasName = "\\" + fqnClasName;
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(phpClass);
        if(scopeForUseOperator == null) {
            return null;
        }

        if(!PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).containsValue(fqnClasName)) {
            PhpAliasImporter.insertUseStatement(fqnClasName, scopeForUseOperator);
        }

        for (Map.Entry<String, String> entry : PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).entrySet()) {
            if(fqnClasName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    public static int getCurrentParameterIndex(PsiElement[] parameters, PsiElement parameter) {
        int i;
        for(i = 0; i < parameters.length; i = i + 1) {
            if(parameters[i].equals(parameter)) {
                return i;
            }
        }

        return -1;
    }
}
