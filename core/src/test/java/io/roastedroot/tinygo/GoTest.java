package io.roastedroot.tinygo;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportFunction;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoTest {

    @Test
    public void mainWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/main-wasi.wasm");
        var module = Parser.parse(wasm);
        var stdout = new ByteArrayOutputStream();
        var wasiOpts = WasiOptions.builder()
                .withStdout(stdout)
                .build();
        var go = Go.builder(module)
                .withWasi(wasiOpts)
                .build();

        // Act
        var result = go.run();

        // Assert
        assertEquals(0, result);
        assertEquals("Hello world!\n", stdout.toString(StandardCharsets.UTF_8));
    }

    // low level API
    @Test
    public void importWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/import-wasi.wasm");
        var module = Parser.parse(wasm);

        var go = Go.builder(module)
                .withWasi()
                .build();

        // Act
        go.run();
        var result = go.exec("operation", new long[] { 321 })[0];

        // Assert
        assertEquals(323, result);
    }

    @Test
    public void usageWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/usage-wasi.wasm");
        var module = Parser.parse(wasm);

        var go = Go.builder(module)
                .withWasi()
                .withAdditionalImport(goInst ->
                        new ImportFunction[]{
                                new HostFunction("mygo", "javaValidate", FunctionType.of(
                                        List.of(ValType.I32),
                                        List.of(ValType.I32)
                                ),
                                        (inst, args) -> {
                                            var ref = (int) args[0];
                                            var str = (String) goInst.getJavaObj(ref);

                                            return new long[] { goInst.allocJavaObj(str.equals("foo")) };
                                        })
                        }
                )
                .build();

        // Act
        go.run();
        var str1 = go.allocJavaObj("foo");
        var str2 = go.allocJavaObj("bar");

        var result1 = ((int) go.exec("usage", new long[] { str1 })[0]) > 0;
        var result2 = ((int) go.exec("usage", new long[] { str2 })[0]) > 0;

        // Assert
        assertTrue(result1);
        assertFalse(result2);
    }

    @Test
    public void callbackWasiExample() {
        // Arrange
        var resetInvoked = new AtomicInteger();
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/callback-wasi.wasm");
        var module = Parser.parse(wasm);

        var go = Go.builder(module)
                .withWasi()
                .withAdditionalImport(goInst ->
                        new ImportFunction[]{
                                new HostFunction("mygo", "reset", FunctionType.of(
                                        List.of(ValType.I32),
                                        List.of()
                                ),
                                        (inst, args) -> {
                                            var ref = (int) args[0];
                                            var str = (String) goInst.getJavaObj(ref);

                                            goInst.setJavaObj(ref, "0");

                                            resetInvoked.set(Integer.valueOf(str));
                                            return null;
                                        })
                        }
                )
                .build();

        // Act
        go.run();

        // Assert
        assertEquals(11, resetInvoked.get());
    }
}
