package de.espend.intellij.php.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpAccessVariableInstruction;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FunctionCallParameterCompletionContributor extends CompletionContributor {
    public FunctionCallParameterCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement originalPosition = completionParameters.getOriginalPosition();

                FunctionReference parentOfType = PsiTreeUtil.getParentOfType(originalPosition, FunctionReference.class);
                if (parentOfType != null) {
                    PsiElement resolve = parentOfType.resolve();

                    if (resolve instanceof Function) {
                        com.jetbrains.php.lang.psi.elements.Parameter[] parameters = ((Function) resolve).getParameters();

                        List<MyParameter> types = new ArrayList<>();

                        for (com.jetbrains.php.lang.psi.elements.Parameter parameter : parameters) {
                            if (!parameter.isOptional()) {
                                types.add(new MyParameter(parameter.getName(), parameter.getType()));
                            }
                        }

                        if (resolve instanceof FunctionImpl) {
                            String name = ((Function) resolve).getName();

                            if ("in_array".equals(name) || "array_search".equals(name)) {
                                MyParameter myParameter = types.get(0);
                                if (myParameter != null) {
                                    types.set(0, new MyParameter(myParameter.name, new PhpType().add(PhpType.STRING).add(PhpType.INT).add(PhpType.FLOAT)));
                                }
                            }

                            if ("array_key_exists".equals(name) || "key_exists".equals(name)) {
                                MyParameter myParameter = types.get(0);
                                if (myParameter != null) {
                                    types.set(0, new MyParameter(myParameter.name, new PhpType().add(PhpType.STRING).add(PhpType.INT).add(PhpType.FLOAT)));
                                }
                            }
                        }

                        Function parentOfType1 = PsiTreeUtil.getParentOfType(parentOfType, Function.class);

                        Map<String, PhpPsiElement> vars = new HashMap<>();

                        PhpControlFlowUtil.processFlow(parentOfType1.getControlFlow(), new PhpInstructionProcessor() {
                            @Override
                            public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
                                vars.put(instruction.getVariableName().toString(), instruction.getAnchor());
                                return super.processAccessVariableInstruction(instruction);
                            }
                        });

                        List<Collection<VariableMatch>> matches = new ArrayList<>();

                        for (int i = 0; i < types.size(); i++) {
                            matches.add(new ArrayList<>());

                            MyParameter myParameter = types.get(i);
                            for (Map.Entry<String, PhpPsiElement> entry : vars.entrySet()) {
                                PhpPsiElement value = entry.getValue();
                                if (!(value instanceof PhpTypedElement)) {
                                    continue;
                                }

                                @NotNull PhpType types1 = ((PhpTypedElement) value).getType();
                                boolean b = myParameter.anyMatch(originalPosition.getProject(), types1);
                                if (b) {
                                    matches.get(i).add(new VariableMatch(entry.getKey(), types1));
                                    //completionResultSet.addElement(LookupElementBuilder.create("foobar"));
                                    //completionResultSet.addElement(LookupElementBuilder.create("$" + entry.getKey()));
                                }
                            }
                        }

                        System.out.println(matches);
                    }


                }
            }
        });
    }

    public static class VariableMatch {
        private final String name;
        private final PhpType phpType;

        public VariableMatch(@NotNull String name, @NotNull PhpType phpType) {
            this.name = name;
            this.phpType = phpType;
        }
    }

    public static class MyParameter {
        private final String name;
        private final PhpType phpType;

        public MyParameter(@NotNull String name, @NotNull PhpType phpType) {
            this.name = name;
            this.phpType = phpType;
        }

        public boolean anyMatch(@NotNull Project project, @NotNull PhpType types) {
            return phpType.isConvertibleFromGlobal(project, types);
        }
    }
}
