package io.roastedroot.tinygo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportFunction;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class GoTest {

    @Test
    public void mainWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/main-wasi.wasm");
        var module = Parser.parse(wasm);
        var stdout = new ByteArrayOutputStream();
        var wasiOpts = WasiOptions.builder().withStdout(stdout).build();
        var go = Go.builder(module).withWasi(wasiOpts).build();

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

        var go = Go.builder(module).withWasi().build();

        // Act
        go.run();
        var result = go.exec("operation", new long[] {321})[0];

        // Assert
        assertEquals(323, result);
    }

    @Test
    public void usageWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/usage-wasi.wasm");
        var module = Parser.parse(wasm);

        var go =
                Go.builder(module)
                        .withWasi()
                        .withAdditionalImport(
                                goInst ->
                                        new ImportFunction[] {
                                            new HostFunction(
                                                    "mygo",
                                                    "javaValidate",
                                                    FunctionType.of(
                                                            List.of(ValType.I32),
                                                            List.of(ValType.I32)),
                                                    (inst, args) -> {
                                                        var ref = (int) args[0];
                                                        var str = (String) goInst.getJavaObj(ref);

                                                        return new long[] {
                                                            goInst.allocJavaObj(str.equals("foo"))
                                                        };
                                                    })
                                        })
                        .build();

        // Act
        go.run();
        var str1 = go.allocJavaObj("foo");
        var str2 = go.allocJavaObj("bar");

        var result1 = ((int) go.exec("usage", new long[] {str1})[0]) > 0;
        var result2 = ((int) go.exec("usage", new long[] {str2})[0]) > 0;

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

        var go =
                Go.builder(module)
                        .withWasi()
                        .withAdditionalImport(
                                goInst ->
                                        new ImportFunction[] {
                                            new HostFunction(
                                                    "mygo",
                                                    "reset",
                                                    FunctionType.of(
                                                            List.of(ValType.I32), List.of()),
                                                    (inst, args) -> {
                                                        var ref = (int) args[0];
                                                        var str = (String) goInst.getJavaObj(ref);

                                                        goInst.setJavaObj(ref, "0");

                                                        resetInvoked.set(Integer.valueOf(str));
                                                        return null;
                                                    })
                                        })
                        .build();

        // Act
        go.run();

        // Assert
        assertEquals(11, resetInvoked.get());
    }

    @Test
    public void exportWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/export-wasi.wasm");
        var module = Parser.parse(wasm);

        var go = Go.builder(module).withWasi().build();

        // Act
        go.run();
        var aRef = go.allocJavaObj("3");
        var bRef = go.allocJavaObj("11");
        var resultRef = (int) go.exec("update", new long[] {aRef, bRef})[0];
        var result = go.getJavaObj(resultRef);

        // Assert
        assertEquals("14", result);
    }

    @Test
    public void exportWasiRuntimeCompilerExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/export-wasi.wasm");
        var module = Parser.parse(wasm);

        var go =
                Go.builder(module)
                        .withWasi()
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();

        // Act
        go.run();
        var aRef = go.allocJavaObj("3");
        var bRef = go.allocJavaObj("11");
        var resultRef = (int) go.exec("update", new long[] {aRef, bRef})[0];
        var result = go.getJavaObj(resultRef);

        // Assert
        assertEquals("14", result);
    }

    @Test
    public void exportWasiBuildTimeCompilerExample() {
        // Arrange
        var module = ExportWasi.load();

        var go = Go.builder(module).withWasi().withMachineFactory(ExportWasi::create).build();

        // Act
        go.run();
        var aRef = go.allocJavaObj("3");
        var bRef = go.allocJavaObj("11");
        var resultRef = (int) go.exec("update", new long[] {aRef, bRef})[0];
        var result = go.getJavaObj(resultRef);

        // Assert
        assertEquals("14", result);
    }

    @Test
    public void slicesWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/slices-wasi.wasm");
        var module = Parser.parse(wasm);
        var stdout = new ByteArrayOutputStream();
        var wasiOpts =
                WasiOptions.builder()
                        .withStdout(stdout)
                        .withArguments(List.of("program-name", "1,2,3"))
                        .build();

        var go = Go.builder(module).withWasi(wasiOpts).build();

        // Act
        go.run();

        // Assert
        assertEquals("[1 2 3]\n", stdout.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void withdepWasiExample() throws Exception {
        // Arrange
        var expectedResult = GoTest.class.getResourceAsStream("/qrcode.png").readAllBytes();
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/withdep-wasi.wasm");
        var module = Parser.parse(wasm);

        var go = Go.builder(module).withWasi().build();

        // Act
        go.run();
        var url = go.allocJavaObj("https://chicory.dev");
        var resultRef = (int) go.exec("genqr", new long[] {url})[0];
        var result = (byte[]) go.getJavaObj(resultRef);

        // Assert
        assertArrayEquals(expectedResult, result);
    }

    @Test
    public void datatypesWasiExample() {
        // Arrange
        var wasm = GoTest.class.getResourceAsStream("/wasm/compiled/datatypes-wasi.wasm");
        var module = Parser.parse(wasm);

        var go = Go.builder(module).withWasi().build();

        // Act
        go.run();

        // String
        var strRef = go.allocJavaObj("hello");
        var outStrRef = (int) go.exec("roundtripString", new long[] {strRef})[0];
        var outStr = (String) go.getJavaObj(outStrRef);
        assertEquals("hello", outStr);

        // Bytes
        var bytesIn = new byte[] {1, 2, 3, 4};
        var bytesRef = go.allocJavaObj(bytesIn);
        var outBytesRef = (int) go.exec("roundtripBytes", new long[] {bytesRef})[0];
        var outBytes = (byte[]) go.getJavaObj(outBytesRef);
        assertArrayEquals(bytesIn, outBytes);

        // Uint32
        var u32In = 123_456_789;
        var u32Ref = go.allocJavaObj(u32In);
        var outU32Ref = (int) go.exec("roundtripUint32", new long[] {u32Ref})[0];
        var outU32 = (int) go.getJavaObj(outU32Ref);
        assertEquals(u32In, outU32);

        // Uint64 (kept within 32-bit safe range due to host return width)
        var u64In = 987_654_321L;
        var u64Ref = go.allocJavaObj(u64In);
        var outU64Ref = (int) go.exec("roundtripUint64", new long[] {u64Ref})[0];
        var outU64 = (long) go.getJavaObj(outU64Ref);
        assertEquals(u64In, outU64);

        // Float32
        var f32In = 1.5f;
        var f32Ref = go.allocJavaObj(f32In);
        var outF32Ref = (int) go.exec("roundtripFloat32", new long[] {f32Ref})[0];
        var outF32 = (float) go.getJavaObj(outF32Ref);
        assertEquals(f32In, outF32);

        // Float64
        var f64In = 3.25d;
        var f64Ref = go.allocJavaObj(f64In);
        var outF64Ref = (int) go.exec("roundtripFloat64", new long[] {f64Ref})[0];
        var outF64 = (double) go.getJavaObj(outF64Ref);
        assertEquals(f64In, outF64);

        // Bool
        var bIn = true;
        var bRef = go.allocJavaObj(bIn);
        var outBRef = (int) go.exec("roundtripBool", new long[] {bRef})[0];
        var outB = (boolean) go.getJavaObj(outBRef);
        assertTrue(outB);
    }
}
