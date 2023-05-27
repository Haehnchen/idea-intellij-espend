package de.espend.intellij.php.utils;

import com.jetbrains.php.codeInsight.PhpScopeHolder;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpAccessVariableInstruction;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ElementsUtil {
    public static Map<String, PhpType> collectVariablesWithTypes(@NotNull PhpScopeHolder scope) {
        Map<String, PhpType> vars = new HashMap<>();

        PhpControlFlowUtil.processFlow(scope.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
                if (instruction.getAnchor() instanceof PhpTypedElement phpTypedElement) {
                    String varName = instruction.getVariableName().toString();
                    vars.putIfAbsent(varName, new PhpType());
                    vars.get(varName).add(phpTypedElement.getType());
                }

                return super.processAccessVariableInstruction(instruction);
            }
        });

        return vars;
    }
}
