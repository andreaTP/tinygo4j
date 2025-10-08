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
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
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
import java.util.Set;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

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

        cu.addImport("io.roastedroot.tinygo4j.Go");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");

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
                                            new SimpleName("floatToLong"),
                                            NodeList.nodeList(new NameExpr("arg" + i))));
                            break;
                        case "double":
                            arguments.add(
                                    new MethodCallExpr(
                                            new NameExpr("Value"),
                                            new SimpleName("doubleToLong"),
                                            NodeList.nodeList(new NameExpr("arg" + i))));
                            break;
                        case "boolean":
                            arguments.add(
                                    new ConditionalExpr(
                                            new NameExpr("arg" + i),
                                            new IntegerLiteralExpr("1"),
                                            new IntegerLiteralExpr("0")));
                            break;
                        default:
                            if (annotatedWith(param, HostRefParam.class)) {
                                arguments.add(
                                        new MethodCallExpr(
                                                new NameExpr("go"),
                                                new SimpleName("allocJavaObj"),
                                                NodeList.nodeList(new NameExpr("arg" + i))));
                            } else {
                                throw new IllegalArgumentException(
                                        "unsupported parameter type: "
                                                + typeLiteral
                                                + "for function "
                                                + member.getSimpleName().toString());
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
                    var primitiveReturn = false;
                    switch (returnType.asString()) {
                        case "int":
                        case "long":
                        case "float":
                        case "double":
                        case "boolean":
                            primitiveReturn = true;
                    }

                    overriddenMethod.setType(returnType);

                    if (executable.getReturnType().toString().equals("boolean")) {
                        methodBody.addStatement(
                                new ReturnStmt(
                                        new EnclosedExpr(
                                                new BinaryExpr(
                                                        new ArrayAccessExpr(
                                                                invocationHandle,
                                                                new IntegerLiteralExpr(0)),
                                                        new IntegerLiteralExpr(0),
                                                        BinaryExpr.Operator.GREATER))));
                    } else if (executable.getReturnType().toString().equals("float")) {
                        methodBody.addStatement(
                                new ReturnStmt(
                                        new MethodCallExpr(
                                                new NameExpr("Value"),
                                                new SimpleName("longToFloat"),
                                                NodeList.nodeList(
                                                        new ArrayAccessExpr(
                                                                invocationHandle,
                                                                new IntegerLiteralExpr(0))))));
                    } else if (executable.getReturnType().toString().equals("double")) {
                        methodBody.addStatement(
                                new ReturnStmt(
                                        new MethodCallExpr(
                                                new NameExpr("Value"),
                                                new SimpleName("longToDouble"),
                                                NodeList.nodeList(
                                                        new ArrayAccessExpr(
                                                                invocationHandle,
                                                                new IntegerLiteralExpr(0))))));
                    } else if (!primitiveReturn
                            && !annotatedWith(executable, ReturnsHostRef.class)) {
                        throw new IllegalArgumentException(
                                "unsupported return type: "
                                        + returnType
                                        + "for function "
                                        + member.getSimpleName().toString());
                    } else if (annotatedWith(executable, ReturnsHostRef.class)) {
                        methodBody.addStatement(
                                new ReturnStmt(
                                        new CastExpr(
                                                returnType,
                                                new MethodCallExpr(
                                                        new NameExpr("go"),
                                                        new SimpleName("getJavaObj"),
                                                        NodeList.nodeList(
                                                                new CastExpr(
                                                                        parseType("int"),
                                                                        new ArrayAccessExpr(
                                                                                invocationHandle,
                                                                                new IntegerLiteralExpr(
                                                                                        0))))))));
                    } else {
                        methodBody.addStatement(
                                new ReturnStmt(
                                        new CastExpr(
                                                returnType,
                                                new ArrayAccessExpr(
                                                        invocationHandle,
                                                        new IntegerLiteralExpr(0)))));
                    }
                } else {
                    methodBody.addStatement(invocationHandle);
                }
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
}
