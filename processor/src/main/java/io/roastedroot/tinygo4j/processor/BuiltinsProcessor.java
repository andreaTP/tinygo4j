package io.roastedroot.tinygo4j.processor;

import static com.github.javaparser.StaticJavaParser.parseType;
import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.roastedroot.tinygo4j.annotations.Builtins;
import io.roastedroot.tinygo4j.annotations.HostFunction;
import io.roastedroot.tinygo4j.annotations.HostRefParam;
import io.roastedroot.tinygo4j.annotations.ReturnsHostRef;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public final class BuiltinsProcessor extends Tinygo4jAbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Builtins.class)) {
            log(NOTE, "Generating Builtins for " + element, null);
            try {
                processBuiltins((TypeElement) element);
            } catch (AbortProcessingException e) {
                // skip type
            }
        }
        return false;
    }

    private void processBuiltins(TypeElement type) {
        var name = type.getAnnotation(Builtins.class).value();
        if (name.isEmpty()) {
            name = type.getSimpleName().toString();
        }

        List<Expression> functions = new ArrayList<>();
        for (Element member : elements().getAllMembers(type)) {
            if (member instanceof ExecutableElement && annotatedWith(member, HostFunction.class)) {
                functions.add(processHostFunction((ExecutableElement) member, name));
            }
        }

        var pkg = getPackageName(type);
        var packageName = pkg.getQualifiedName().toString();
        var cu = (pkg.isUnnamed()) ? new CompilationUnit() : new CompilationUnit(packageName);
        if (!pkg.isUnnamed()) {
            cu.setPackageDeclaration(packageName);
            cu.addImport(type.getQualifiedName().toString());
        }

        cu.addImport(List.class);
        cu.addImport(Function.class);

        cu.addImport("com.dylibso.chicory.wasm.types.FunctionType");
        cu.addImport("com.dylibso.chicory.wasm.types.ValType");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");
        cu.addImport("com.dylibso.chicory.runtime.HostFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.runtime.ImportFunction");

        cu.addImport("io.roastedroot.tinygo4j.Go");

        var typeName = type.getSimpleName().toString();
        var processorName = new StringLiteralExpr(getClass().getName());
        var classDef =
                cu.addClass(typeName + "_Builtins")
                        .setPublic(true)
                        .setFinal(true)
                        .addSingleMemberAnnotation(Generated.class, processorName);

        classDef.addConstructor().setPrivate(true);

        var newGoFunctions =
                new ArrayCreationExpr(
                        parseType("ImportFunction"),
                        new NodeList<>(new ArrayCreationLevel()),
                        new ArrayInitializerExpr(NodeList.nodeList(functions)));

        var builtinsCreationHandle =
                new LambdaExpr(
                                new Parameter(parseType("Go"), new SimpleName("goInst")),
                                newGoFunctions)
                        .setEnclosingParameters(true);

        classDef.addMethod("toAdditionalImports")
                .setPublic(true)
                .setStatic(true)
                .addParameter(typeName, "javaBuiltins")
                .setType("Function<Go, ImportFunction[]>")
                .setBody(new BlockStmt(new NodeList<>(new ReturnStmt(builtinsCreationHandle))));

        String prefix = (pkg.isUnnamed()) ? "" : packageName + ".";
        String qualifiedName = prefix + type.getSimpleName() + "_Builtins";
        try (Writer writer = filer().createSourceFile(qualifiedName, type).openWriter()) {
            writer.write(cu.printer(printer()).toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", qualifiedName, e), null);
        }
    }

    private Expression extractWasmType(String name) {
        switch (name) {
            case "float":
                return new FieldAccessExpr(new NameExpr("ValType"), "F32");
            case "double":
                return new FieldAccessExpr(new NameExpr("ValType"), "F64");
            case "long":
                return new FieldAccessExpr(new NameExpr("ValType"), "I64");
            default:
                return new FieldAccessExpr(new NameExpr("ValType"), "I32");
        }
    }

    private Expression processHostFunction(ExecutableElement executable, String moduleName) {
        // compute function name
        var name = executable.getAnnotation(HostFunction.class).value();
        if (name.isEmpty()) {
            name = executable.getSimpleName().toString();
        }

        // compute parameter types and argument conversions
        NodeList<Expression> paramTypes = new NodeList<>();
        // duplicated to automatically compute arguments
        NodeList<Expression> arguments = new NodeList<>();
        for (VariableElement parameter : executable.getParameters()) {
            switch (parameter.asType().toString()) {
                case "int":
                    arguments.add(new CastExpr(parseType("int"), argExpr(paramTypes.size())));
                    break;
                case "long":
                    arguments.add(argExpr(paramTypes.size()));
                    break;
                case "float":
                    arguments.add(
                            new MethodCallExpr(
                                    new NameExpr("Value"),
                                    new SimpleName("longToFloat"),
                                    NodeList.nodeList(argExpr(paramTypes.size()))));
                    break;
                case "double":
                    arguments.add(
                            new MethodCallExpr(
                                    new NameExpr("Value"),
                                    new SimpleName("longToDouble"),
                                    NodeList.nodeList(argExpr(paramTypes.size()))));
                    break;
                case "boolean":
                    arguments.add(
                            new BinaryExpr(
                                    argExpr(paramTypes.size()),
                                    new IntegerLiteralExpr(0),
                                    BinaryExpr.Operator.GREATER));
                    break;
                default:
                    var typeLiteral = parameter.asType().toString();
                    if (annotatedWith(parameter, HostRefParam.class)) {
                        var jObj =
                                new MethodCallExpr(
                                        new NameExpr("goInst"),
                                        new SimpleName("getJavaObj"),
                                        NodeList.nodeList(
                                                new CastExpr(
                                                        parseType("int"),
                                                        argExpr(paramTypes.size()))));
                        arguments.add(new CastExpr(parseType(typeLiteral), jObj));
                    } else {
                        throw new IllegalArgumentException(
                                "unsupported parameter type: " + typeLiteral);
                    }
            }
            paramTypes.add(extractWasmType(parameter.asType().toString()));
        }

        // compute return type and conversion
        boolean hasReturn = extractHasReturn(executable);
        Expression returnType = null;

        // function invocation
        Expression invocation =
                new MethodCallExpr(
                        new NameExpr("javaBuiltins"),
                        executable.getSimpleName().toString(),
                        arguments);

        // convert return value
        BlockStmt handleBody = new BlockStmt();
        if (!hasReturn) {
            handleBody.addStatement(invocation).addStatement(new ReturnStmt(new NullLiteralExpr()));
        } else {
            returnType = extractWasmType(executable.getReturnType().toString());
            VariableDeclarator result;

            switch (executable.getReturnType().toString()) {
                case "int":
                case "long":
                case "float":
                case "double":
                    result =
                            new VariableDeclarator(
                                    parseType(executable.getReturnType().toString()),
                                    "result",
                                    invocation);
                    break;
                case "boolean":
                    result =
                            new VariableDeclarator(
                                    parseType("int"),
                                    "result",
                                    new ConditionalExpr(
                                            invocation,
                                            new IntegerLiteralExpr(1),
                                            new IntegerLiteralExpr(0)));
                    break;
                default:
                    if (annotatedWith(executable, ReturnsHostRef.class)) {
                        // TODO: verify what happens when I want to re-use an incoming reference?
                        result =
                                new VariableDeclarator(
                                        parseType("int"),
                                        "result",
                                        new MethodCallExpr(
                                                new NameExpr("goInst"),
                                                new SimpleName("allocJavaObj"),
                                                NodeList.nodeList(invocation)));
                        break;
                    } else {
                        throw new IllegalArgumentException(
                                "unsupported return type: "
                                        + executable.getReturnType().toString());
                    }
            }

            handleBody.addStatement(new ExpressionStmt(new VariableDeclarationExpr(result)));

            // Convert result to long for WASM interface
            Expression returnValue;
            switch (executable.getReturnType().toString()) {
                case "double":
                    returnValue =
                            new MethodCallExpr(
                                    new NameExpr("Value"),
                                    new SimpleName("doubleToLong"),
                                    NodeList.nodeList(new NameExpr("result")));
                    break;
                case "float":
                    returnValue =
                            new MethodCallExpr(
                                    new NameExpr("Value"),
                                    new SimpleName("floatToLong"),
                                    NodeList.nodeList(new NameExpr("result")));
                    break;
                default:
                    returnValue = new NameExpr("result");
                    break;
            }

            handleBody.addStatement(
                    new ReturnStmt(
                            new ArrayCreationExpr(
                                    parseType("long"),
                                    new NodeList<>(new ArrayCreationLevel()),
                                    new ArrayInitializerExpr(NodeList.nodeList(returnValue)))));
        }

        // lambda for js function binding
        var handle =
                new LambdaExpr()
                        .addParameter("Instance", "inst")
                        .addParameter("long[]", "args")
                        .setEnclosingParameters(true)
                        .setBody(handleBody);

        // function signature
        var functionSignature =
                new MethodCallExpr(
                        new NameExpr("FunctionType"),
                        "of",
                        NodeList.nodeList(
                                new MethodCallExpr(new NameExpr("List"), "of", paramTypes),
                                (hasReturn)
                                        ? new MethodCallExpr(
                                                new NameExpr("List"),
                                                "of",
                                                NodeList.nodeList(returnType))
                                        : new MethodCallExpr(
                                                new NameExpr("List"), "of", NodeList.nodeList())));

        // create Js function
        var function =
                new ObjectCreationExpr()
                        .setType("HostFunction")
                        .addArgument(new StringLiteralExpr(moduleName))
                        .addArgument(new StringLiteralExpr(name))
                        .addArgument(functionSignature)
                        .addArgument(handle);

        function.setLineComment("");

        return function;
    }

    private static Expression argExpr(int n) {
        return new ArrayAccessExpr(new NameExpr("args"), new IntegerLiteralExpr(n));
    }
}
