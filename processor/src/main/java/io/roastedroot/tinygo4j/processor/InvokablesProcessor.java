package io.roastedroot.tinygo4j.processor;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseType;
import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.roastedroot.tinygo4j.annotations.GuestFunction;
import io.roastedroot.tinygo4j.annotations.HostRefParam;
import io.roastedroot.tinygo4j.annotations.Invokables;
import io.roastedroot.tinygo4j.annotations.ReturnsHostRef;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public final class InvokablesProcessor extends Tinygo4jAbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Invokables.class)) {
            log(NOTE, "Generating Invokables for " + element, null);
            try {
                processInvokables((TypeElement) element);
            } catch (AbortProcessingException e) {
                // skip type
            }
        }
        return false;
    }

    private void processInvokables(TypeElement type) {
        var moduleName = type.getAnnotation(Invokables.class).value();
        if (moduleName.isEmpty()) {
            moduleName = type.getSimpleName().toString();
        }

        var pkg = getPackageName(type);
        var packageName = pkg.getQualifiedName().toString();
        var cu = (pkg.isUnnamed()) ? new CompilationUnit() : new CompilationUnit(packageName);
        if (!pkg.isUnnamed()) {
            cu.setPackageDeclaration(packageName);
            cu.addImport(type.getQualifiedName().toString());
        }

        cu.addImport("io.roastedroot.tinygo.Go");

        var typeName = type.getSimpleName().toString();
        var processorName = new StringLiteralExpr(getClass().getName());
        var className = typeName + "_Invokables";
        var classDef =
                cu.addClass(className)
                        .setPublic(true)
                        .setFinal(true)
                        .addImplementedType(typeName)
                        .addSingleMemberAnnotation(Generated.class, processorName);

        classDef.addField(parseType("Go"), "go", Modifier.Keyword.FINAL);

        var constructor = classDef.addConstructor().addParameter("Go", "go").setPrivate(true);

        constructor
                .createBody()
                .addStatement(
                        new AssignExpr(
                                new FieldAccessExpr(new ThisExpr(), "go"),
                                new NameExpr("go"),
                                AssignExpr.Operator.ASSIGN));

        List<Expression> functions = new ArrayList<>();
        for (Element member : elements().getAllMembers(type)) {
            if (member instanceof ExecutableElement && annotatedWith(member, GuestFunction.class)) {
                var name = member.getAnnotation(GuestFunction.class).value();
                if (name.isEmpty()) {
                    name = member.getSimpleName().toString();
                }

                var executable = (ExecutableElement) member;

                var overriddenMethod =
                        classDef.addMethod(
                                        member.getSimpleName().toString(), Modifier.Keyword.PUBLIC)
                                .addAnnotation(Override.class);

                NodeList<Expression> arguments = NodeList.nodeList();
                for (int i = 0; i < executable.getParameters().size(); i++) {
                    var param = executable.getParameters().get(i);
                    var typeLiteral = param.asType().toString();
                    overriddenMethod.addParameter(typeLiteral, "arg" + i);
                    switch (typeLiteral) {
                        case "long":
                        case "int":
                            arguments.add(new NameExpr("arg" + i));
                            break;
                        case "float":
                            arguments.add(
                                    new MethodCallExpr(
                                            new NameExpr("Value"),
                                            new SimpleName("longToFloat"),
                                            NodeList.nodeList(new NameExpr("arg" + i))));
                            break;
                        case "double":
                            arguments.add(
                                    new MethodCallExpr(
                                            new NameExpr("Value"),
                                            new SimpleName("longToDouble"),
                                            NodeList.nodeList(new NameExpr("arg" + i))));
                            break;
                        case "boolean":
                            throw new IllegalArgumentException("TODO: implement me");
                        default:
                            if (annotatedWith(param, HostRefParam.class)) {
                                throw new IllegalArgumentException("TODO: implement me");
                            } else {
                                throw new IllegalArgumentException(
                                        "unsupported parameter type: " + typeLiteral);
                            }
                    }
                }

                var methodBody = overriddenMethod.createBody();

                var invocationHandle =
                        new MethodCallExpr(
                                new NameExpr("go"),
                                new SimpleName("exec"),
                                NodeList.nodeList(
                                        new StringLiteralExpr(name),
                                        new ArrayCreationExpr(
                                                parseType("long"),
                                                new NodeList<>(new ArrayCreationLevel()),
                                                new ArrayInitializerExpr(
                                                        NodeList.nodeList(arguments)))));

                var hasReturn = extractHasReturn(executable);
                if (hasReturn) {
                    var returnType = parseType(executable.getReturnType().toString());

                    overriddenMethod.setType(returnType);
                    methodBody.addStatement(
                            new ReturnStmt(
                                    new CastExpr(
                                            returnType,
                                            new ArrayAccessExpr(
                                                    invocationHandle, new IntegerLiteralExpr(0)))));

                    //                    if (annotatedWith(executable, ReturnsHostRef.class)) {
                    //
                    //                    } else {
                    //
                    //                    }
                } else {
                    methodBody.addStatement(invocationHandle);
                }

                functions.add(processGuestFunction((ExecutableElement) member));
            }
        }

        classDef.addMethod("create", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
                .setType(typeName)
                .addParameter(parseType("Go"), "go")
                .setBody(
                        new BlockStmt()
                                .addStatement(
                                        new ReturnStmt(
                                                new ObjectCreationExpr(
                                                        null,
                                                        parseClassOrInterfaceType(className),
                                                        NodeList.nodeList(new NameExpr("go"))))));

        String prefix = (pkg.isUnnamed()) ? "" : packageName + ".";
        String qualifiedName = prefix + type.getSimpleName() + "_Invokables";
        try (Writer writer = filer().createSourceFile(qualifiedName, type).openWriter()) {
            writer.write(cu.printer(printer()).toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", qualifiedName, e), null);
        }
    }

    private Expression addPrimitiveReturn(String typeLiteral) {
        return new FieldAccessExpr(new NameExpr(typeLiteral), "class");
    }

    // TODO: review this implementation is wrong
    private Expression extractReturn(ExecutableElement executable) {
        String returnName = executable.getReturnType().toString();
        Expression returnType;
        switch (returnName) {
            case "void":
                returnType = new FieldAccessExpr(new NameExpr("java.lang.Void"), "class");
                break;
            case "int":
                returnType = addPrimitiveReturn("java.lang.Integer");
                break;
            case "long":
                returnType = addPrimitiveReturn("java.lang.Long");
                break;
            case "double":
                returnType = addPrimitiveReturn("java.lang.Double");
                break;
            case "float":
                returnType = addPrimitiveReturn("java.lang.Float");
                break;
            case "boolean":
                returnType = addPrimitiveReturn("java.lang.Boolean");
                break;
            default:
                if (annotatedWith(executable, ReturnsHostRef.class)) {
                    var javaRefType = "io.roastedroot.quickjs4j.core.HostRef";
                    returnType = new FieldAccessExpr(new NameExpr(javaRefType), "class");
                } else {
                    returnType = new FieldAccessExpr(new NameExpr(returnName), "class");
                }
                break;
        }
        return returnType;
    }

    NodeList<Expression> extractParameters(ExecutableElement executable) {
        // compute parameter types and argument conversions
        NodeList<Expression> paramTypes = new NodeList<>();
        for (VariableElement parameter : executable.getParameters()) {
            switch (parameter.asType().toString()) {
                case "int":
                    paramTypes.add(new FieldAccessExpr(new NameExpr("java.lang.Integer"), "class"));
                    break;
                case "long":
                    paramTypes.add(new FieldAccessExpr(new NameExpr("java.lang.Long"), "class"));
                    break;
                case "double":
                    paramTypes.add(new FieldAccessExpr(new NameExpr("java.lang.Double"), "class"));
                    break;
                case "float":
                    paramTypes.add(new FieldAccessExpr(new NameExpr("java.lang.Float"), "class"));
                    break;
                case "boolean":
                    paramTypes.add(new FieldAccessExpr(new NameExpr("java.lang.Boolean"), "class"));
                    break;
                default:
                    var typeLiteral = parameter.asType().toString();
                    if (annotatedWith(parameter, HostRefParam.class)) {
                        var javaRefType = "io.roastedroot.quickjs4j.core.HostRef";
                        paramTypes.add(new FieldAccessExpr(new NameExpr(javaRefType), "class"));
                    } else {
                        paramTypes.add(new FieldAccessExpr(new NameExpr(typeLiteral), "class"));
                    }
            }
        }
        return paramTypes;
    }

    private Expression processGuestFunction(ExecutableElement executable) {
        // compute function name
        var name = executable.getAnnotation(GuestFunction.class).value();
        if (name.isEmpty()) {
            name = executable.getSimpleName().toString();
        }

        // compute parameter types and argument conversions
        NodeList<Expression> paramTypes = extractParameters(executable);

        // compute return type and conversion
        Expression returnType = extractReturn(executable);

        // create Js function
        var function =
                new ObjectCreationExpr()
                        .setType("GuestFunction")
                        .addArgument(new StringLiteralExpr(name))
                        .addArgument(new MethodCallExpr(new NameExpr("List"), "of", paramTypes))
                        .addArgument(returnType);

        function.setLineComment("");
        return function;
    }
}
