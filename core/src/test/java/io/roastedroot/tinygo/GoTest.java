package io.roastedroot.tinygo;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GoTest {

    @Test
    public void mainUnknownExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/main-wasm-unknown.wasm");
        var module = Parser.parse(wasm);
        var go = Go.builder(module).build();

        // Act
        var result = go.run();

        // Assert
        assertEquals(0, result);
    }

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
    public void importUnknownExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/import-wasm-unknown.wasm");
        var module = Parser.parse(wasm);

        var go = Go.builder(module)
                .build();

        // Act
        go.run();
        var result = go.exec("operation", new long[] { 321 })[0];

        // Assert
        assertEquals(323, result);
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
                .build();

        // Act
        go.run();
        var result = go.exec("usage", new long[] {})[0];

        // Assert
        assertEquals(987, result);
    }
}
