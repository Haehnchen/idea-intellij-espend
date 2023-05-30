package de.espend.intellij.php.completion.utils;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.formatter.PhpCodeStyleSettings;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.refactoring.PhpNameStyle;
import com.jetbrains.php.refactoring.PhpNameUtil;
import de.espend.intellij.php.completion.dict.AnonymousFunctionMatch;
import de.espend.intellij.php.completion.dict.AnonymousFunctionWithParameter;
import de.espend.intellij.php.utils.ElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnonymousFunctionUtil {

    @NotNull
    public static List<AnonymousFunctionMatch> getAnonymousFunctionMatchesForArrayMap(@NotNull CompletionParameters completionParameters, PsiElement startScope) {
        List<AnonymousFunctionMatch> matches1 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches2 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches3 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches4 = new ArrayList<>();

        Project project = startScope.getProject();

        for (Map.Entry<String, PhpType> entry : ElementsUtil.collectVariablesWithTypes(startScope).entrySet()) {
            PhpType value = entry.getValue();

            if (new PhpType().add(PhpType.ARRAY).isConvertibleFromGlobal(project, value)) {
                for (PhpType phpType : new PhpType[] {PhpType.INT, PhpType.STRING, PhpType.FLOAT}) {
                    if (value.containsAll(new PhpType().add(phpType.toString() + "[]"))) {
                        matches1.add(new AnonymousFunctionMatch(entry.getKey(), StringUtils.stripStart(phpType.toString(), "\\"), 100));
                    }
                }
            }

            for (String type : PhpIndex.getInstance(project).completeType(project, PhpType.from(value), new HashSet<>()).getTypes()) {
                // we only want: \Foo, but not primitives like \string
                if (!new PhpType().add(type).filterPrimitives().isEmpty() && type.startsWith("\\") && !type.endsWith("[]")) {
                    String substring = type.substring(1);

                    Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN("\\" + substring);
                    if (anyByFQN.isEmpty()) {
                        continue;
                    }

                    PhpClass phpClass = anyByFQN.iterator().next();
                    boolean isThisScope = "this".equals(entry.getKey());

                    Collection<PhpNamedElement> phpNamedElements = new ArrayList<>();
                    phpNamedElements.addAll(getValidFields(phpClass, isThisScope));
                    phpNamedElements.addAll(getValidMethods(phpClass, isThisScope));

                    for (PhpNamedElement phpNamedElement : phpNamedElements) {
                        String resolvedArrayFqn = findFirstArrayClassFqn(project, phpNamedElement.getType());
                        if (resolvedArrayFqn == null) {
                            continue;
                        }

                        String resolvedFqn = resolvedArrayFqn.substring(0, resolvedArrayFqn.length() - 2);
                        Collection<PhpClass> phpClassesResolved = PhpIndex.getInstance(project).getAnyByFQN(resolvedFqn);
                        if (phpClassesResolved.isEmpty()) {
                            continue;
                        }

                        PhpClass phpClassResolved = phpClassesResolved.iterator().next();

                        AnonymousFunctionWithParameter.ReferenceType field1 = phpNamedElement instanceof Field
                            ? AnonymousFunctionWithParameter.ReferenceType.FIELD
                            : AnonymousFunctionWithParameter.ReferenceType.METHOD;

                        String parameterArrayReference = entry.getKey() + "->" + phpNamedElement.getName();
                        if (phpNamedElement instanceof Method) {
                            parameterArrayReference += "()";
                        }

                        int weight = 30;
                        matches2.add(new AnonymousFunctionMatch(
                            phpNamedElement.getName(),
                            phpClassResolved.getFQN(),
                            weight,
                            null,
                            field1,
                            parameterArrayReference
                        ));
                    }
                }

                visitForeignMatchesDirectMatch(project, matches1, matches3, matches4, entry.getKey(), type);
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
    private static String findFirstArrayClassFqn(@NotNull Project project, @NotNull PhpType field) {
        return PhpIndex.getInstance(project)
            .completeType(project, field, new HashSet<>())
            .getTypes()
            .stream()
            .filter(s -> s.startsWith("\\") && s.endsWith("[]"))
            .findFirst().orElse(null);
    }

    private static void visitForeignMatchesDirectMatch(@NotNull Project project, List<AnonymousFunctionMatch> matches1, List<AnonymousFunctionMatch> matches3, List<AnonymousFunctionMatch> matches4, @NotNull String key, String type) {
        if (!(type.startsWith("\\") && type.endsWith("[]"))) {
            return;
        }

        String substring = type.substring(1, type.length() - 2);

        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN("\\" + substring);
        if (anyByFQN.isEmpty()) {
            return;
        }

        matches1.add(new AnonymousFunctionMatch(key,  "\\" + substring, 80));

        int index = 0;

        PhpClass phpClass = anyByFQN.iterator().next();

        for (Field field : getValidFields(phpClass, false)) {
            PhpType firstPrimitive = getFindFirstPrimitive(project, field.getType());
            if (firstPrimitive == null) {
                continue;
            }

            int weight = getWeightForField(field) + index++;
            matches3.add(new AnonymousFunctionMatch(key, firstPrimitive.toString(), weight, field.getName(), AnonymousFunctionWithParameter.ReferenceType.FIELD, null));
        }

        index += 1;

        for (Method field : getValidMethods(phpClass, false)) {
            PhpType firstPrimitive = getFindFirstPrimitive(project, field.getType());
            if (firstPrimitive == null) {
                continue;
            }

            int weight = getWeightForMethod(field) + index++;
            matches4.add(new AnonymousFunctionMatch(key, firstPrimitive.toString(), weight, field.getName(), AnonymousFunctionWithParameter.ReferenceType.METHOD, null));
        }
    }

    private static int getWeightForMethod(@NotNull Method field) {
        int weight = 0;

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

        return weight;
    }

    private static int getWeightForField(@NotNull Field field) {
        int weight = 0;

        String nameNormalized = field.getName();
        if (nameNormalized.endsWith("id") || nameNormalized.endsWith("key")) {
            weight += 10;
        }

        if (field.getType().isBoolean()) {
            weight -= 30;
        }

        return weight;
    }

    public static List<AnonymousFunctionMatch> getAnonymousFunctionMatchesForType(@NotNull PhpTypedElement phpTypedElement) {
        List<AnonymousFunctionMatch> matches1 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches2 = new ArrayList<>();
        List<AnonymousFunctionMatch> matches3 = new ArrayList<>();

        Project project = phpTypedElement.getProject();

        PhpType type = phpTypedElement.getType();
        if (new PhpType().add(PhpType.ARRAY).isConvertibleFromGlobal(project, type)) {
            for (PhpType phpType : new PhpType[] {PhpType.INT, PhpType.STRING, PhpType.FLOAT}) {
                if (type.containsAll(new PhpType().add(phpType.toString() + "[]"))) {
                    matches1.add(new AnonymousFunctionMatch(phpTypedElement.getName(), StringUtils.stripStart(phpType.toString(), "\\"), 100));
                }
            }
        }

        for (String s : PhpIndex.getInstance(project).completeType(project, type, new HashSet<>()).getTypes()) {
            String name = phpTypedElement.getName();
            visitForeignMatchesDirectMatch(project, matches1, matches2, matches3, name, s);
        }

        List<AnonymousFunctionMatch> matches = new ArrayList<>();

        matches.addAll(matches1.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).limit(5).toList());
        matches.addAll(matches2.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).limit(5).toList());
        matches.addAll(matches3.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).limit(5).toList());

        return matches.stream().sorted(Comparator.comparingInt(AnonymousFunctionMatch::weight).reversed()).toList();
    }

    @Nullable
    private static PhpType getFindFirstPrimitive(@NotNull Project project, @NotNull PhpType value) {
        for (PhpType phpType : new PhpType[] {PhpType.INT, PhpType.STRING, PhpType.FLOAT}) {
            if (PhpIndex.getInstance(project).completeType(project, value, new HashSet<>()).containsAll(new PhpType().add(phpType.toString() + "[]"))) {
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

    @NotNull
    public static Collection<Method> getValidMethods(PhpClass phpClass, boolean isThisScope) {
        return phpClass.getMethods().stream().filter(method -> {
            if (method.isStatic()) {
                return false;
            }

            return isThisScope || method.getModifier().isPublic();
        }).collect(Collectors.toList());
    }

    @NotNull
    public static Collection<Field> getValidFields(@NotNull PhpClass next, boolean isThisScope) {
        return next.getFields().stream().filter(field -> {
            if (field.isConstant()) {
                return false;
            }

            return isThisScope || field.getModifier().isPublic();
        }).collect(Collectors.toList());
    }


    public static Collection<PhpClass> getClassesFromTyp(@NotNull Project project, @NotNull PhpType phpType) {
        Collection<PhpClass> phpClasses = new HashSet<>();

        for (String type : PhpType.from(phpType).filterPrimitives().getTypes()) {
            if (type.startsWith("\\") && !type.endsWith("[]")) {
                Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN(type);

                // first wins
                if (!anyByFQN.isEmpty()) {
                    phpClasses.add(anyByFQN.iterator().next());
                }
            }
        }

        return phpClasses;
    }
}
