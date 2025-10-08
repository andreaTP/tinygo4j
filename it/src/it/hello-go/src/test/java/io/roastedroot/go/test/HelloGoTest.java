package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.Parser;
import io.roastedroot.tinygo.Go;
import io.roastedroot.tinygo4j.annotations.Builtins;
import io.roastedroot.tinygo4j.annotations.GuestFunction;
import io.roastedroot.tinygo4j.annotations.HostFunction;
import io.roastedroot.tinygo4j.annotations.HostRefParam;
import io.roastedroot.tinygo4j.annotations.Invokables;
import io.roastedroot.tinygo4j.annotations.ReturnsHostRef;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HelloGoTest {

    @Invokables
    interface GoApi {
        @GuestFunction
        void test1();
    }

    @Builtins("from_java")
    class JavaApi {
        public boolean invoked;
        public boolean refInvoked;

        @ReturnsHostRef
        @HostFunction("my_java_func")
        public String add(int x, int y) {
            var sum = x + y;
            return "hello " + sum;
        }

        @HostFunction("my_java_check")
        public void check(@HostRefParam String value) {
            invoked = true;
            assertEquals("hello 42", value);
        }

        @ReturnsHostRef
        @HostFunction("my_java_ref")
        public String myRef() {
            return "a pure java string";
        }

        @HostFunction("my_java_ref_check")
        public void myRefCheck(@HostRefParam String value) {
            refInvoked = true;
            assertEquals("a pure java string", value);
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
                                    "../../../../core/src/test/resources/wasm/compiled/hello-it-wasi.wasm"));
            this.javaApi = new JavaApi();
            this.go =
                    Go.builder(module)
                            .withWasi()
                            .withAdditionalImport(
                                    JavaApi_Builtins.toAdditionalImports(this.javaApi))
                            .build();
            this.goApi = GoApi_Invokables.create(go);
        }
    }

    @Test
    public void test1() {
        // Arrange
        var helloGo = new GoTest();

        // Act
        helloGo.goApi.test1();

        // Assert
        assertTrue(helloGo.javaApi.invoked);
    }

    // Those tests should be ported to Go
    // @Test
    // public void helloJsModule() {
    //     // Arrange
    //     var helloJs = new JsTest();

    //     // Act
    //     helloJs.exec("from_java.my_java_check(from_java.my_java_func(40, 2));");

    //     // Assert
    //     assertTrue(helloJs.isInvoked());
    // }

    // @Test
    // public void useJavaRefs() {
    //     // Arrange
    //     var helloJs = new JsTest();

    //     // Act
    //     helloJs.exec("from_java.my_java_ref_check(from_java.my_java_ref());");

    //     // assert
    //     assertTrue(helloJs.isRefInvoked());
    // }

    // @Test
    // public void useInvokables() {
    //     // Arrange
    //     var helloJs = new JsTest();

    //     // Act
    //     var result = helloJs.sub(5, 2);

    //     // assert
    //     assertEquals(3, result);
    // }
}
