package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.roastedroot.tinygo4j.annotations.Builtins;
import io.roastedroot.tinygo4j.annotations.GuestFunction;
import io.roastedroot.tinygo4j.annotations.HostFunction;
import io.roastedroot.tinygo4j.annotations.HostRefParam;
import io.roastedroot.tinygo4j.annotations.Invokables;
import io.roastedroot.tinygo4j.annotations.ReturnsHostRef;

class HelloGoTest {

    @Invokables
    interface GoApi {
        @GuestFunction
        int sub(int x, int y);
    }

    @Builtins("from_java")
    class JavaApi {
        public boolean invoked;
        public boolean refInvoked;

        @HostFunction("my_java_func")
        public String add(int x, int y) {
            var sum = x + y;
            return "hello " + sum;
        }

        @HostFunction("my_java_check")
        public void check(String value) {
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

        private final GoApi goApi;

        GoTest() {
            var module = null; // TODO: craft a go project that will use this API
            var javaApi = new JavaApi();
            var go =
                    Go.builder(module)
                            .withWasi()
                            .withAdditionalImport(JavaApi_Builtins.toBuiltins(this.javaApi))
                            .build();
            this.goApi = GoApi_Invokables.create(go);
        }

        public void exec(String code) {
            runner.compileAndExec(code);
        }

        public boolean isInvoked() {
            return javaApi.invoked;
        }

        public boolean isRefInvoked() {
            return javaApi.refInvoked;
        }

        public int sub(int x, int y) {
            return goApi.sub(x, y);
        }
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
