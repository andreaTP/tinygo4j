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
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.CastExpr;
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

        cu.addImport("com.dylibso.chicory.wasm.types.FunctionType");
        cu.addImport("com.dylibso.chicory.wasm.types.ValType");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");
        cu.addImport("com.dylibso.chicory.runtime.HostFunction");
        cu.addImport("com.dylibso.chicory.runtime.ImportFunction");
        cu.addImport(List.class);

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
                        new Parameter(parseType("Go"), new SimpleName("goInst")), newGoFunctions);

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

    private void addPrimitiveParam(
            String typeLiteral, NodeList<Expression> paramTypes, NodeList<Expression> arguments) {
        var type = parseType(typeLiteral);
        arguments.add(new CastExpr(type, argExpr(paramTypes.size())));
        paramTypes.add(new FieldAccessExpr(new NameExpr(typeLiteral), "class"));
    }

    private Expression addPrimitiveReturn(String typeLiteral) {
        return new FieldAccessExpr(new NameExpr(typeLiteral), "class");
    }

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
                    addPrimitiveParam("java.lang.Integer", paramTypes, arguments);
                    break;
                case "long":
                    addPrimitiveParam("java.lang.Long", paramTypes, arguments);
                    break;
                case "double":
                    addPrimitiveParam("java.lang.Double", paramTypes, arguments);
                    break;
                case "float":
                    addPrimitiveParam("java.lang.Float", paramTypes, arguments);
                    break;
                case "boolean":
                    addPrimitiveParam("java.lang.Boolean", paramTypes, arguments);
                    break;
                default:
                    var typeLiteral = parameter.asType().toString();
                    var type = parseType(parameter.asType().toString());
                    arguments.add(new CastExpr(type, argExpr(paramTypes.size())));
                    if (annotatedWith(parameter, HostRefParam.class)) {
                        var javaRefType = "io.roastedroot.quickjs4j.core.HostRef";
                        paramTypes.add(new FieldAccessExpr(new NameExpr(javaRefType), "class"));
                    } else {
                        paramTypes.add(new FieldAccessExpr(new NameExpr(typeLiteral), "class"));
                    }
            }
        }

        // compute return type and conversion
        Expression returnType = extractReturn(executable);
        boolean hasReturn = extractHasReturn(executable);

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
            var result =
                    new VariableDeclarator(
                            parseType(executable.getReturnType().toString()), "result", invocation);
            handleBody
                    .addStatement(new ExpressionStmt(new VariableDeclarationExpr(result)))
                    .addStatement(new ReturnStmt(new NameExpr("result")));
        }

        // lambda for js function binding
        var handle =
                new LambdaExpr()
                        .addParameter(new Parameter(parseType("List<Object>"), "args"))
                        .setEnclosingParameters(true)
                        .setBody(handleBody);

        // create Js function
        var function =
                new ObjectCreationExpr()
                        .setType("HostFunction")
                        .addArgument(new StringLiteralExpr(name))
                        .addArgument(new MethodCallExpr(new NameExpr("List"), "of", paramTypes))
                        .addArgument(returnType)
                        .addArgument(handle);

        function.setLineComment("");

        return function;
    }

    private static Expression argExpr(int n) {
        return new MethodCallExpr(
                new NameExpr("args"),
                new SimpleName("get"),
                NodeList.nodeList(new IntegerLiteralExpr(String.valueOf(n))));
    }
}
