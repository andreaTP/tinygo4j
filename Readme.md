### TinyGo4J

**TinyGo4J** lets you run TinyGo‑compiled WebAssembly and interoperate with Java on the JVM. It uses the [Chicory](https://github.com/dylibso/chicory) runtime and provides both a low‑level API and an annotation‑driven, high‑level, type‑safe binding layer.

> **Experimental**: This project is experimental and we are looking for feedback on the design and implementation. Try it out and let us know what you think!

### Why TinyGo4J?

- **Interop made easy**: pass primitives and opaque Java object references between Go and Java.
- **Pure Java**: no native deps; works anywhere the JVM runs.
- **WASI support**: run TinyGo WASI modules with configurable stdio/args/fs/env.

### Project setup (Maven)

Add dependencies and enable annotation processing:

```xml
<dependencies>
  <dependency>
    <groupId>io.roastedroot</groupId>
    <artifactId>tinygo4j-core</artifactId>
  </dependency>
  <dependency>
    <groupId>io.roastedroot</groupId>
    <artifactId>tinygo4j-annotations</artifactId>
    <scope>provided</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>io.roastedroot</groupId>
            <artifactId>tinygo4j-processor</artifactId>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### Quick start

Run a TinyGo WASI module and capture stdout:

```java
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasi.WasiOptions;
import io.roastedroot.tinygo4j.Go;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

var wasm = MyApp.class.getResourceAsStream("main.wasm");
var module = Parser.parse(wasm);
var stdout = new ByteArrayOutputStream();
var wasi = WasiOptions.builder().withStdout(stdout).build();

var go = Go.builder(module).withWasi(wasi).build();
int exitCode = go.run();
assert exitCode == 0;
System.out.println(stdout.toString(StandardCharsets.UTF_8)); // Hello world!\n
```

Corresponding TinyGo program:

```go
package main

import "fmt"

func main() {
    fmt.Println("Hello world!")
}
```

### Calling Go from Java

Define an interface for Go exports and call it from Java:

```java
import com.dylibso.chicory.wasm.Parser;
import io.roastedroot.tinygo4j.Go;
import io.roastedroot.tinygo4j.annotations.*;
import java.nio.file.Path;

@Invokables
interface MathApi {
  @GuestFunction("add")
  int add(int a, int b);
}

var module = Parser.parse(Path.of("add.wasm"));
var go = Go.builder(module).withWasi().build();
var math = MathApi_Invokables.create(go);

int result = math.add(3, 11);
System.out.println(result); // 14
```

Corresponding TinyGo program:

```go
package main

//export add
func add(a int32, b int32) int32 { return a + b }

func main() {}
```

### Calling Java from Go

Expose Java methods to Go via annotations, then execute Go code that calls them:

```java
import com.dylibso.chicory.wasm.Parser;
import io.roastedroot.tinygo4j.Go;
import io.roastedroot.tinygo4j.annotations.*;
import java.nio.file.Path;

@Builtins("from_java")
class JavaApi {
  @ReturnsHostRef
  @HostFunction("my_java_func")
  public String add(int x, int y) { return "hello " + (x + y); }

  @HostFunction("my_java_check")
  public void check(@HostRefParam String value) { assert ("hello 42".equals(value)); }
}

@Invokables
interface GoApi {
  @GuestFunction void test();
}

var module = Parser.parse(Path.of("hello-java.wasm"));
var go = Go.builder(module)
    .withWasi()
    .withAdditionalImport(JavaApi_Builtins.toAdditionalImports(new JavaApi()))
    .build();
var goApi = GoApi_Invokables.create(go);

goApi.test();
```

Corresponding TinyGo snippet (imports Java host functions and calls them):

```go
package main

//go:wasmimport from_java my_java_func
func my_java_func(x int32, y int32) int32

//go:wasmimport from_java my_java_check
func my_java_check(ref int32)

//export test
func test() {
    ref := my_java_func(40, 2)
    my_java_check(ref)
}

func main() {}
```

Notes:
- `@Invokables` generates a type-safe client to call Go exports.
- `@Builtins` + `@HostFunction` generate WASM imports mapped to Java methods.
- `@HostRefParam` / `@ReturnsHostRef` pass opaque Java objects by reference (no serialization) across the boundary.

### Working with JavaRefs in Go

Use the Go dependency for convenient JavaRef handling:

```bash
go get github.com/roastedroot/tinygo4j
```

Example Go code using the JavaRef API:

```go
package main

import "github.com/roastedroot/tinygo4j"

//export processString
func processString(strRef tinygo4j.JavaRef) bool {
    // Convert JavaRef to Go string
    str := strRef.AsString()

    // Create new JavaRef with Go string
    newRef := tinygo4j.Alloc().Set().String(str)

    // Clean up
    newRef.Free()

    // Return boolean result
    return len(str) > 0
}

func main() {}
```

### Compile with TinyGo

Compile your Go code with TinyGo targeting WASI (examples under `core/src/test/resources/wasm`):

```bash
tinygo build -o your.wasm -target=wasip1 ./path/to/main.go
```

Basic support is also available for `wasm-unknown-unknown`:

```bash
tinygo build -o your.wasm -target=wasm-unknown ./path/to/main.go
```

Ensure your Go module exports functions matching your `@GuestFunction` signatures and imports host functions under the module name used in `@Builtins`.

### Acknowledgements

- [`TinyGo`](https://tinygo.org/) – Go compiler for tiny places
- [`Chicory`](https://chicory.dev/) – Java WebAssembly runtime
