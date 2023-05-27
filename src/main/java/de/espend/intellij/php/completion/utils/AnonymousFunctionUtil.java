package de.espend.intellij.php.completion.utils;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.PhpScopeHolder;
import com.jetbrains.php.lang.formatter.PhpCodeStyleSettings;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.refactoring.PhpNameStyle;
import com.jetbrains.php.refactoring.PhpNameUtil;
import de.espend.intellij.php.completion.dict.AnonymousFunction;
import de.espend.intellij.php.completion.dict.AnonymousFunctionMatch;
import de.espend.intellij.php.utils.ElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnonymousFunctionUtil {

    @NotNull
    public static List<AnonymousFunctionMatch> getAnonymousFunctionMatchesForArrayMap(@NotNull CompletionParameters completionParameters, PsiElement startScope) {
        PhpScopeHolder parentOfType1 = PsiTreeUtil.getParentOfType(startScope, Function.class);
        if (parentOfType1 == null) {
            parentOfType1 = PsiTreeUtil.getParentOfType(startScope, PhpScopeHolder.class);
        }

        if (parentOfType1 == null) {
            return Collections.emptyList();
        }

        List<AnonymousFunctionMatch> matches1 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches2 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches3 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches4 = new ArrayList<>();

        for (Map.Entry<String, PhpType> entry : ElementsUtil.collectVariablesWithTypes(parentOfType1).entrySet()) {
            PhpType value = entry.getValue();

            if (new PhpType().add(PhpType.ARRAY).isConvertibleFromGlobal(startScope.getProject(), value)) {
                for (PhpType phpType : new PhpType[] {PhpType.INT, PhpType.STRING, PhpType.FLOAT}) {
                    if (value.containsAll(new PhpType().add(phpType.toString() + "[]"))) {
                        matches1.add(new AnonymousFunctionMatch(entry.getKey(), StringUtils.stripStart(phpType.toString(), "\\"), 100));
                    }
                }
            }

            for (String type : PhpType.from(value).getTypes()) {
                if (!new PhpType().add(type).filterPrimitives().isEmpty()) {
                    if (type.startsWith("\\") && !type.endsWith("[]")) {
                        String substring = type.substring(1);

                        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(completionParameters.getPosition().getProject()).getAnyByFQN("\\" + substring);
                        if (!anyByFQN.isEmpty()) {
                            PhpClass next = anyByFQN.iterator().next();
                            for (Field field : next.getFields()) {
                                if (field.isConstant()) {
                                    continue;
                                }

                                if (!"this".equals(entry.getKey()) && !field.getModifier().isPublic()) {
                                    continue;
                                }

                                String s1 = field.getType().getTypes().stream().filter(s -> s.startsWith("\\") && s.endsWith("[]")).findFirst().orElse(null);
                                if (s1 != null) {
                                    String substring2 = s1.substring(0, s1.length() - 2);

                                    Collection<PhpClass> anyByFQN1 = PhpIndex.getInstance(completionParameters.getPosition().getProject()).getAnyByFQN(substring2);
                                    if (!anyByFQN1.isEmpty()) {
                                        PhpClass next1 = anyByFQN1.iterator().next();

                                        int weight = 30;
                                        matches2.add(new AnonymousFunctionMatch(
                                            field.getName(),
                                            next1.getFQN(),
                                            weight,
                                            null,
                                            AnonymousFunction.ReferenceType.FIELD,
                                            entry.getKey() + "->" + field.getName()
                                        ));
                                    }
                                }
                            }

                            for (Method field : next.getMethods()) {
                                if (field.isStatic()) {
                                    continue;
                                }

                                if (!"this".equals(entry.getKey()) && !field.getModifier().isPublic()) {
                                    continue;
                                }

                                String s1 = field.getType().getTypes().stream().filter(s -> s.startsWith("\\") && s.endsWith("[]")).findFirst().orElse(null);
                                if (s1 != null) {
                                    String substring2 = s1.substring(0, s1.length() - 2);

                                    Collection<PhpClass> anyByFQN1 = PhpIndex.getInstance(completionParameters.getPosition().getProject()).getAnyByFQN(substring2);
                                    if (!anyByFQN1.isEmpty()) {
                                        PhpClass next1 = anyByFQN1.iterator().next();

                                        int weight = 30;

                                        matches2.add(new AnonymousFunctionMatch(
                                            field.getName(),
                                            next1.getFQN(),
                                            weight,
                                            null,
                                            AnonymousFunction.ReferenceType.METHOD,
                                            entry.getKey() + "->" + field.getName() + "()")
                                        );
                                    }
                                }
                            }
                        }
                    }
                }

                if (type.startsWith("\\") && type.endsWith("[]")) {
                    String substring = type.substring(1, type.length() - 2);

                    if (!new PhpType().add(substring).filterPrimitives().isEmpty()) {
                        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(completionParameters.getPosition().getProject()).getAnyByFQN("\\" + substring);
                        if (!anyByFQN.isEmpty()) {
                            matches1.add(new AnonymousFunctionMatch(entry.getKey(),  "\\" + substring, 80));

                            int index = 0;

                            PhpClass next = anyByFQN.iterator().next();
                            for (Field field : next.getFields()) {
                                if (!field.isConstant() && field.getModifier().isPublic()) {
                                    PhpType findFirstPrimitives1 = getFindFirstPrimitives(field.getType());
                                    if (findFirstPrimitives1 == null) {
                                        continue;
                                    }

                                    int weight = index;

                                    String nameNormalized = field.getName();
                                    if (nameNormalized.endsWith("id") || nameNormalized.endsWith("key")) {
                                        weight += 10;
                                    }

                                    if (field.getType().isBoolean()) {
                                        weight -= 30;
                                    }

                                    index += 1;
                                    matches3.add(new AnonymousFunctionMatch(entry.getKey(), findFirstPrimitives1.toString().substring(1), weight, field.getName(), AnonymousFunction.ReferenceType.FIELD, null));
                                }
                            }

                            index += 1;
                            for (Method field : next.getMethods()) {
                                if (!field.isStatic() && field.getModifier().isPublic()) {
                                    PhpType findFirstPrimitives1 = getFindFirstPrimitives(field.getType());
                                    if (findFirstPrimitives1 == null) {
                                        continue;
                                    }

                                    int weight = index;

                                    String nameNormalized = field.getName();

                                    if (nameNormalized.startsWith("get")) {
                                        nameNormalized = nameNormalized.substring(0, 3);
                                    } else if (nameNormalized.startsWith("is")) {
                                        nameNormalized = nameNormalized.substring(0, 2);
                                    }

                                    if (nameNormalized.endsWith("id") || nameNormalized.endsWith("key")) {
                                        weight += 10;
                                    }

                                    if (field.getType().isBoolean()) {
                                        weight -= 30;
                                    }

                                    index += 1;
                                    matches4.add(new AnonymousFunctionMatch(entry.getKey(), findFirstPrimitives1.toString().substring(1), weight, field.getName(), AnonymousFunction.ReferenceType.METHOD, null));
                                }
                            }
                        }
                    }
                }
            }
        }

        List<AnonymousFunctionMatch> matches = new ArrayList<>();

        matches.addAll(matches1.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).limit(5).toList());
        matches.addAll(matches2.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).limit(5).toList());
        matches.addAll(matches3.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).limit(5).toList());
        matches.addAll(matches4.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).limit(5).toList());

        return matches.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).toList();
    }

    @Nullable
    private static PhpType getFindFirstPrimitives(@NotNull PhpType value) {
        for (PhpType phpType : new PhpType[] {PhpType.INT, PhpType.STRING, PhpType.FLOAT}) {
            if (value.containsAll(new PhpType().add(phpType.toString() + "[]"))) {
                return phpType;
            }
        }

        return null;
    }

    public static String getVariableName(@NotNull PhpCodeStyleSettings phpCodeStyleSettings, @NotNull String name) {
        String trimmedType = StringUtil.trimEnd(name, "Interface");
        trimmedType = StringUtil.trimEnd(trimmedType, "Abstract");
        trimmedType = StringUtil.trimEnd(trimmedType, "Decorator");
        trimmedType = StringUtil.trimEnd(trimmedType, "Factory");

        if (trimmedType.isBlank()) {
            return WordUtils.uncapitalize(name);
        }

        List<String> strings1 = PhpNameStyle.DECAPITALIZE.withStyle(phpCodeStyleSettings.VARIABLE_NAMING_STYLE).generateNames(PhpNameUtil.splitName(trimmedType));
        if (strings1.isEmpty()) {
            return WordUtils.uncapitalize(name);
        }

        return strings1.get(0);
    }
}
