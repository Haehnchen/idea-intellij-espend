package de.espend.intellij.php.completion;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.formatter.PhpCodeStyleSettings;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import de.espend.intellij.php.completion.dict.AnonymousFunctionWithParameter;
import de.espend.intellij.php.completion.dict.AnonymousFunction;
import de.espend.intellij.php.completion.dict.AnonymousFunctionMatch;
import de.espend.intellij.php.completion.lookupElement.AnonymousFunctionLookupElement;
import de.espend.intellij.php.completion.lookupElement.AnonymousFunctionWithParameterLookupElement;
import de.espend.intellij.php.completion.utils.AnonymousFunctionUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClosureArrowCompletionContributor extends CompletionContributor {
    public ClosureArrowCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement originalPosition = completionParameters.getOriginalPosition();
                if (originalPosition == null) {
                    return;
                }

                PsiElement position = completionParameters.getPosition();

                PsiElement parent = position.getParent();

                // @TODO: clean it up after collected all its needed use cases

                // "fo("
                // "fo(fn"
                boolean constantReference = parent instanceof ConstantReference;

                // fo($
                boolean variable = parent instanceof Variable;

                if (!constantReference && !variable) {
                    return;
                }

                PsiElement parent1 = parent.getParent();
                if (!(parent1 instanceof ParameterList)) {
                    return;
                }

                FunctionReference parentOfType = PsiTreeUtil.getParentOfType(originalPosition, FunctionReference.class);
                if (parentOfType == null) {
                    return;
                }

                if (constantReference) {
                    if (parentOfType instanceof FunctionReferenceImpl && "array_map".equals(parentOfType.getName())) {
                        PsiElement parameter = ((ParameterList) parent1).getParameter(1);
                        if (parameter == null) {
                            arrayMap(completionParameters, completionResultSet, parentOfType);
                        } else {
                            arrayMapFiltered(completionParameters, completionResultSet, parameter);
                        }

                        return;
                    }

                    if (parentOfType instanceof FunctionReferenceImpl && "array_filter".equals(parentOfType.getName())) {
                        PsiElement parameter = ((ParameterList) parent1).getParameter(0);
                        if (parameter != null) {
                            arrayFilterFiltered(completionParameters, completionResultSet, parameter);
                        }
                    }
                }

                if (variable && originalPosition.getNode().getElementType() == PhpTokenTypes.DOLLAR) {
                    if (parentOfType instanceof FunctionReferenceImpl && "array_filter".equals(parentOfType.getName())) {
                        arrayFilter(completionParameters, completionResultSet, parentOfType);
                    }
                }
            }
        });
    }

    private static void arrayMap(@NotNull CompletionParameters completionParameters, @NotNull CompletionResultSet completionResultSet, @NotNull FunctionReference parentOfType) {
        PhpCodeStyleSettings customSettings = CodeStyle.getCustomSettings(completionParameters.getPosition().getContainingFile(), PhpCodeStyleSettings.class);

        for (AnonymousFunctionMatch match : AnonymousFunctionUtil.getAnonymousFunctionMatchesForArrayMap(completionParameters, parentOfType)) {
            String parameter = getFunctionParameterFromArgument(customSettings, match);

            LookupElement element = null;

            String prefix = completionResultSet.getPrefixMatcher().getPrefix();
            if (prefix.isBlank() || "fn".startsWith(prefix)) {
                element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunctionWithParameter(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ARROW, false));
            } else if (prefix.isBlank() || "static fn".startsWith(prefix)) {
                element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunctionWithParameter(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ARROW, true));
            } else if ("function".startsWith(prefix)) {
                element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunctionWithParameter(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ANONYMOUS, false));
            } else if ("static function".startsWith(prefix)) {
                element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunctionWithParameter(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ANONYMOUS, true));
            }

            if (element != null) {
                completionResultSet.addElement(element);
            }
        }
    }

    private static void arrayFilter(@NotNull CompletionParameters completionParameters, @NotNull CompletionResultSet completionResultSet, @NotNull FunctionReference parentOfType) {
        PhpCodeStyleSettings customSettings = CodeStyle.getCustomSettings(completionParameters.getPosition().getContainingFile(), PhpCodeStyleSettings.class);

        for (AnonymousFunctionMatch match : AnonymousFunctionUtil.getAnonymousFunctionMatchesForArrayMap(completionParameters, parentOfType)) {
            String parameter = getFunctionParameterFromArgument(customSettings, match);
            completionResultSet.addElement(new AnonymousFunctionWithParameterLookupElement(new AnonymousFunctionWithParameter(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ARROW, false, true)));
        }
    }

    private static void arrayFilterFiltered(@NotNull CompletionParameters completionParameters, @NotNull CompletionResultSet completionResultSet, @NotNull PsiElement psiElement) {
        if (psiElement instanceof PhpTypedElement phpTypedElement) {
            PhpCodeStyleSettings customSettings = CodeStyle.getCustomSettings(completionParameters.getPosition().getContainingFile(), PhpCodeStyleSettings.class);

            for (AnonymousFunctionMatch match : AnonymousFunctionUtil.getAnonymousFunctionMatchesForType(phpTypedElement)) {
                String parameter = getFunctionParameterFromArgument(customSettings, match);

                LookupElement element = null;

                String prefix = completionResultSet.getPrefixMatcher().getPrefix();
                if (prefix.isBlank() || "fn".startsWith(prefix)) {
                    element = new AnonymousFunctionLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ARROW, false));
                } else if (prefix.isBlank() || "static fn".startsWith(prefix)) {
                    element = new AnonymousFunctionLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ARROW, true));
                } else if ("function".startsWith(prefix)) {
                    element = new AnonymousFunctionLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ANONYMOUS, false));
                } else if ("static function".startsWith(prefix)) {
                    element = new AnonymousFunctionLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ANONYMOUS, true));
                }

                if (element != null) {
                    completionResultSet.addElement(element);
                }
            }
        }
    }

    private static void arrayMapFiltered(@NotNull CompletionParameters completionParameters, @NotNull CompletionResultSet completionResultSet, @NotNull PsiElement psiElement) {
        if (psiElement instanceof PhpTypedElement phpTypedElement) {
            PhpCodeStyleSettings customSettings = CodeStyle.getCustomSettings(completionParameters.getPosition().getContainingFile(), PhpCodeStyleSettings.class);

            for (AnonymousFunctionMatch match : AnonymousFunctionUtil.getAnonymousFunctionMatchesForType(phpTypedElement)) {
                String parameter = getFunctionParameterFromArgument(customSettings, match);

                AnonymousFunctionWithParameterLookupElement element = null;

                String prefix = completionResultSet.getPrefixMatcher().getPrefix();
                if (prefix.isBlank() || "fn".startsWith(prefix)) {
                    element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ARROW, false));
                } else if (prefix.isBlank() || "static fn".startsWith(prefix)) {
                    element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ARROW, true));
                } else if ("function".startsWith(prefix)) {
                    element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ANONYMOUS, false));
                } else if ("static function".startsWith(prefix)) {
                    element = new AnonymousFunctionWithParameterLookupElement(new AnonymousFunction(match, parameter, AnonymousFunctionWithParameter.FunctionTyp.ANONYMOUS, true));
                }

                if (element != null) {
                    completionResultSet.addElement(element);
                }
            }
        }
    }

    @NotNull
    private static String getFunctionParameterFromArgument(@NotNull PhpCodeStyleSettings customSettings, @NotNull AnonymousFunctionMatch match) {
        String parameter = null;

        if (match.type().contains("\\")) {
            parameter = AnonymousFunctionUtil.getVariableName(customSettings, StringUtils.substringAfterLast(match.type(), "\\"));
        }

        if (StringUtils.isNotBlank(parameter)) {
            return parameter;
        }

        // items => item
        parameter = StringUtil.unpluralize(match.parameterArray());
        if (StringUtils.isNotBlank(parameter)) {
            return parameter;
        }

        if (match.parameterArray().length() > 0) {
            return match.parameterArray().substring(0, 1);
        }

        // string => s
        parameter = match.type().replaceAll("(\\W)", "");
        if (StringUtils.isNotBlank(parameter)) {
            return parameter;
        }

        return "i";
    }
}
