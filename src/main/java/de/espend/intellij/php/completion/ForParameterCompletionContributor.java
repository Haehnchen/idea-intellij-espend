package de.espend.intellij.php.completion;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.formatter.PhpCodeStyleSettings;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.refactoring.PhpNameStyle;
import com.jetbrains.php.refactoring.PhpNameUtil;
import de.espend.intellij.php.completion.utils.AnonymousFunctionUtil;
import de.espend.intellij.php.utils.ElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ForParameterCompletionContributor extends CompletionContributor {
    public ForParameterCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                PsiElement originalPosition = completionParameters.getOriginalPosition();
                if (originalPosition == null) {
                    return;
                }

                if (originalPosition.getNode().getElementType() == PhpTokenTypes.IDENTIFIER || originalPosition.getNode().getElementType() == PhpElementTypes.FOR) {
                    System.out.println(result.getPrefixMatcher().getPrefix());
                    PsiElement parent = originalPosition.getParent();
                    if (parent instanceof ConstantReference || parent instanceof FunctionReference) {
                        String prefix = result.getPrefixMatcher().getPrefix();
                        if (prefix.startsWith("fo")) {
                            visitVariableForForeach(completionParameters, result, originalPosition);
                        }
                    }
                }
            }
        });
    }


    private void visitVariableForForeach(@NotNull CompletionParameters completionParameters, CompletionResultSet result, PsiElement originalPosition) {
        Project project = originalPosition.getProject();
        PhpCodeStyleSettings customSettings = CodeStyle.getCustomSettings(completionParameters.getPosition().getContainingFile(), PhpCodeStyleSettings.class);

        for (Map.Entry<String, PhpType> entry : ElementsUtil.collectVariablesWithTypes(originalPosition).entrySet()) {
            if (new PhpType().add(PhpType.ARRAY).isConvertibleFromGlobal(project, entry.getValue())) {
                String variableName = StringUtil.unpluralize(entry.getKey());
                if (variableName == null || variableName.isBlank()) {
                    variableName = entry.getKey();
                }

                result.addElement(LookupElementBuilder.create("foreach ($" + entry.getKey() + " as $" + variableName + ") {}"));
            }

            for (PhpClass phpClass : AnonymousFunctionUtil.getClassesFromTyp(project, entry.getValue())) {
                boolean isThisScope = "this".equals(entry.getKey());

                for (Method method : AnonymousFunctionUtil.getValidMethods(phpClass, isThisScope)) {
                    if (PhpIndex.getInstance(project).completeType(project, method.getType(), new HashSet<>()).isConvertibleFrom(project, PhpType.ARRAY_TRAVERSABLE_TYPE)) {
                        String s = method.getName();

                        Matcher matcher = Pattern.compile("get_*([A-Z].*)").matcher(s);
                        if (matcher.find()) {
                            s = matcher.group(1);
                        }

                        String variableName = StringUtil.unpluralize(s);
                        if (variableName == null || variableName.isBlank()) {
                            variableName = s;
                        }

                        List<String> strings = PhpNameStyle.DECAPITALIZE.withStyle(customSettings.VARIABLE_NAMING_STYLE).generateNames(PhpNameUtil.splitName(variableName));

                        result.addElement(LookupElementBuilder.create("foreach ($" + entry.getKey() + "->" + method.getName() + "() as $" + strings.get(0) + ") {}"));
                    }
                }

                for (Field field : AnonymousFunctionUtil.getValidFields(phpClass, isThisScope)) {
                    if (PhpIndex.getInstance(project).completeType(project, field.getType(), new HashSet<>()).isConvertibleFrom(project, PhpType.ARRAY_TRAVERSABLE_TYPE)) {
                        String variableName = StringUtil.unpluralize(field.getName());
                        if (variableName == null || variableName.isBlank()) {
                            variableName = field.getName();
                        }

                        List<String> strings = PhpNameStyle.DECAPITALIZE.withStyle(customSettings.VARIABLE_NAMING_STYLE).generateNames(PhpNameUtil.splitName(variableName));

                        result.addElement(LookupElementBuilder.create("foreach ($" + entry.getKey() + "->" + field.getName() + " as $" + strings.get(0) + ") {}"));
                    }
                }
            }
        }
    }
}
