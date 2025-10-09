package io.roastedroot.go.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.Parser;
import io.roastedroot.tinygo4j.Go;
import io.roastedroot.tinygo4j.annotations.Builtins;
import io.roastedroot.tinygo4j.annotations.GuestFunction;
import io.roastedroot.tinygo4j.annotations.HostFunction;
import io.roastedroot.tinygo4j.annotations.HostRefParam;
import io.roastedroot.tinygo4j.annotations.Invokables;
import io.roastedroot.tinygo4j.annotations.ReturnsHostRef;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TypesTest {

    @Invokables
    interface GoApi {
        // Void-to-void guest functions
        @GuestFunction("test_int_to_java")
        void testIntToJava();

        @GuestFunction("test_long_to_java")
        void testLongToJava();

        @GuestFunction("test_double_to_java")
        void testDoubleToJava();

        @GuestFunction("test_float_to_java")
        void testFloatToJava();

        @GuestFunction("test_bool_to_java")
        void testBoolToJava();

        @GuestFunction("test_int_from_java")
        void testIntFromJava();

        @GuestFunction("test_long_from_java")
        void testLongFromJava();

        @GuestFunction("test_double_from_java")
        void testDoubleFromJava();

        @GuestFunction("test_float_from_java")
        void testFloatFromJava();

        @GuestFunction("test_bool_from_java")
        void testBoolFromJava();

        @GuestFunction("test_ref_to_java")
        void testRefToJava();

        @GuestFunction("test_ref_from_java")
        void testRefFromJava();

        @GuestFunction("test_ref_roundtrip")
        void testRefRoundtrip();

        // Guest functions with parameters and return values
        @GuestFunction("test_int_param_return")
        int testIntParamReturn(int value);

        @GuestFunction("test_long_param_return")
        long testLongParamReturn(long value);

        @GuestFunction("test_double_param_return")
        double testDoubleParamReturn(double value);

        @GuestFunction("test_float_param_return")
        float testFloatParamReturn(float value);

        @GuestFunction("test_bool_param_return")
        boolean testBoolParamReturn(boolean value);

        @ReturnsHostRef
        @GuestFunction("test_ref_param_return")
        String testRefParamReturn(@HostRefParam String value);

        // Guest functions with multiple parameters
        @GuestFunction("test_multi_int_params")
        int testMultiIntParams(int a, int b);

        @GuestFunction("test_multi_long_params")
        long testMultiLongParams(long a, long b);

        @GuestFunction("test_multi_double_params")
        double testMultiDoubleParams(double a, double b);

        @GuestFunction("test_multi_float_params")
        float testMultiFloatParams(float a, float b);

        @GuestFunction("test_multi_bool_params")
        boolean testMultiBoolParams(boolean a, boolean b);

        @GuestFunction("test_mixed_params")
        int testMixedParams(int i, long l, double d, float f, boolean b);
    }

    @Builtins("from_java")
    class JavaApi {
        // Host functions for primitive types as parameters
        @HostFunction("test_int_param")
        public void testIntParam(int value) {
            // This method is called with different values depending on the test
            // For testIntToJava: expects 42
            // For testIntFromJava: expects 101 (100 + 1)
            if (value == 42) {
                assertEquals(42, value);
            } else if (value == 101) {
                assertEquals(101, value);
            } else {
                throw new AssertionError("Unexpected int value: " + value);
            }
        }

        @HostFunction("test_long_param")
        public void testLongParam(long value) {
            // For testLongToJava: expects max int64
            // For testLongFromJava: expects 199 (200 - 1)
            if (value == 9223372036854775807L) {
                assertEquals(9223372036854775807L, value);
            } else if (value == 199L) {
                assertEquals(199L, value);
            } else {
                throw new AssertionError("Unexpected long value: " + value);
            }
        }

        @HostFunction("test_double_param")
        public void testDoubleParam(double value) {
            // For testDoubleToJava: expects π
            // For testDoubleFromJava: expects 3.0 (1.5 * 2.0)
            if (Math.abs(value - 3.141592653589793) < 0.000000000000001) {
                assertEquals(3.141592653589793, value, 0.000000000000001);
            } else if (Math.abs(value - 3.0) < 0.000000000000001) {
                assertEquals(3.0, value, 0.000000000000001);
            } else {
                throw new AssertionError("Unexpected double value: " + value);
            }
        }

        @HostFunction("test_float_param")
        public void testFloatParam(float value) {
            // For testFloatToJava: expects e
            // For testFloatFromJava: expects 1.25 (2.5 / 2.0)
            if (Math.abs(value - 2.718281828459045f) < 0.0000001f) {
                assertEquals(2.718281828459045f, value, 0.0000001f);
            } else if (Math.abs(value - 1.25f) < 0.0000001f) {
                assertEquals(1.25f, value, 0.0000001f);
            } else {
                throw new AssertionError("Unexpected float value: " + value);
            }
        }

        @HostFunction("test_bool_param")
        public void testBoolParam(boolean value) {
            // For testBoolToJava: expects true
            // For testBoolFromJava: expects true (!false)
            assertTrue(value);
        }

        // Host functions returning primitive types
        @HostFunction("test_int_return")
        public int testIntReturn() {
            return 100;
        }

        @HostFunction("test_long_return")
        public long testLongReturn() {
            return 200L;
        }

        @HostFunction("test_double_return")
        public double testDoubleReturn() {
            return 1.5;
        }

        @HostFunction("test_float_return")
        public float testFloatReturn() {
            return 2.5f;
        }

        @HostFunction("test_bool_return")
        public boolean testBoolReturn() {
            return false;
        }

        // Host functions for references
        @HostFunction("test_ref_param")
        public void testRefParam(@HostRefParam String value) {
            assertEquals("Hello from Java", value);
        }

        @ReturnsHostRef
        @HostFunction("test_ref_return")
        public String testRefReturn() {
            return "Hello from Java";
        }

        @HostFunction("test_ref_check")
        public void testRefCheck(@HostRefParam String value) {
            assertEquals("Hello from Java", value);
        }
    }

    class GoTest {
        private final Go go;
        private final GoApi goApi;
        private final JavaApi javaApi;

        GoTest() {
            var module =
                    Parser.parse(
                            Path.of(
                                    "../../../../core/src/test/resources/wasm/compiled/types-test-tinygo-wasip1.wasm"));
            this.javaApi = new JavaApi();
            this.go =
                    Go.builder(module)
                            .withWasi()
                            .withAdditionalImport(
                                    JavaApi_Builtins.toAdditionalImports(this.javaApi))
                            .build();
            this.goApi = GoApi_Invokables.create(go);
            go.run();
        }
    }

    @Test
    public void testIntToJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - the assertion is done in the JavaApi method
        typesTest.goApi.testIntToJava();
    }

    @Test
    public void testLongToJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - the assertion is done in the JavaApi method
        typesTest.goApi.testLongToJava();
    }

    @Test
    public void testDoubleToJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - the assertion is done in the JavaApi method
        typesTest.goApi.testDoubleToJava();
    }

    @Test
    public void testFloatToJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - the assertion is done in the JavaApi method
        typesTest.goApi.testFloatToJava();
    }

    @Test
    public void testBoolToJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - the assertion is done in the JavaApi method
        typesTest.goApi.testBoolToJava();
    }

    @Test
    public void testIntFromJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - Go should call testIntParam with 101 (100 + 1)
        typesTest.goApi.testIntFromJava();
    }

    @Test
    public void testLongFromJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - Go should call testLongParam with 199 (200 - 1)
        typesTest.goApi.testLongFromJava();
    }

    @Test
    public void testDoubleFromJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - Go should call testDoubleParam with 3.0 (1.5 * 2.0)
        typesTest.goApi.testDoubleFromJava();
    }

    @Test
    public void testFloatFromJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - Go should call testFloatParam with 1.25 (2.5 / 2.0)
        typesTest.goApi.testFloatFromJava();
    }

    @Test
    public void testBoolFromJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - Go should call testBoolParam with true (!false)
        typesTest.goApi.testBoolFromJava();
    }

    @Test
    public void testRefToJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - the assertion is done in the JavaApi method
        typesTest.goApi.testRefToJava();
    }

    @Test
    public void testRefFromJava() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - the assertion is done in the JavaApi method
        typesTest.goApi.testRefFromJava();
    }

    @Test
    public void testRefRoundtrip() {
        // Arrange
        var typesTest = new GoTest();

        // Act & Assert - This test verifies that references can be passed back and forth
        typesTest.goApi.testRefRoundtrip();
    }

    // Tests for guest functions with parameters and return values
    @Test
    public void testIntParamReturn() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        int result = typesTest.goApi.testIntParamReturn(42);

        // Assert
        assertEquals(43, result); // 42 + 1
    }

    @Test
    public void testLongParamReturn() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        long result = typesTest.goApi.testLongParamReturn(100L);

        // Assert
        assertEquals(99L, result); // 100 - 1
    }

    @Test
    public void testDoubleParamReturn() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        double result = typesTest.goApi.testDoubleParamReturn(3.14);

        // Assert
        assertEquals(6.28, result, 0.000000000000001); // 3.14 * 2.0
    }

    @Test
    public void testFloatParamReturn() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        float result = typesTest.goApi.testFloatParamReturn(4.0f);

        // Assert
        assertEquals(2.0f, result, 0.0000001f); // 4.0 / 2.0
    }

    @Test
    public void testBoolParamReturn() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        boolean result = typesTest.goApi.testBoolParamReturn(true);

        // Assert
        assertFalse(result); // !true
    }

    @Test
    public void testRefParamReturn() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        String result = typesTest.goApi.testRefParamReturn("Hello");

        // Assert
        assertEquals("Hello", result); // Should return the same reference
    }

    // Tests for guest functions with multiple parameters
    @Test
    public void testMultiIntParams() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        int result = typesTest.goApi.testMultiIntParams(10, 5);

        // Assert
        assertEquals(15, result); // 10 + 5
    }

    @Test
    public void testMultiLongParams() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        long result = typesTest.goApi.testMultiLongParams(20L, 5L);

        // Assert
        assertEquals(15L, result); // 20 - 5
    }

    @Test
    public void testMultiDoubleParams() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        double result = typesTest.goApi.testMultiDoubleParams(3.0, 2.0);

        // Assert
        assertEquals(6.0, result, 0.000000000000001); // 3.0 * 2.0
    }

    @Test
    public void testMultiFloatParams() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        float result = typesTest.goApi.testMultiFloatParams(8.0f, 2.0f);

        // Assert
        assertEquals(4.0f, result, 0.0000001f); // 8.0 / 2.0
    }

    @Test
    public void testMultiBoolParams() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        boolean result = typesTest.goApi.testMultiBoolParams(true, false);

        // Assert
        assertFalse(result); // true && false
    }

    @Test
    public void testMixedParams() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        int result = typesTest.goApi.testMixedParams(1, 2L, 3.0, 4.0f, true);

        // Assert
        assertEquals(10, result); // 1 + 2 + 3 + 4 = 10 (when b is true)
    }

    @Test
    public void testMixedParamsFalse() {
        // Arrange
        var typesTest = new GoTest();

        // Act
        int result = typesTest.goApi.testMixedParams(1, 2L, 3.0, 4.0f, false);

        // Assert
        assertEquals(0, result); // Should return 0 when b is false
    }
}
