package io.roastedroot.tinygo;

import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

public final class Go {
    private final Instance instance;
    private final RefStore refs = new RefStore();

    private final ExportFunction mallocFn;

    private Go(
            WasmModule module,
            Function<Instance, Machine> machineFactory,
            ImportFunction[] wasi,
            Function<Go, ImportFunction[]> additionalImports,
            boolean defaultImports) {
        this.instance =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addFunction(wasi)
                                        .addFunction(additionalImports.apply(this))
                                        .addFunction(
                                                (defaultImports)
                                                        ? defaultImports(this)
                                                        : new ImportFunction[0])
                                        .build())
                        .withMachineFactory(machineFactory)
                        .withMemoryFactory(ByteArrayMemory::new)
                        .withStart(false)
                        .build();
        // TODO: this breaks on wasm-unknown
        this.mallocFn = instance.exports().function("malloc");
    }

    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public Object getJavaObj(int ref) {
        return refs.get(ref);
    }

    public int allocJavaObj(Object v) {
        return refs.registerRef(v);
    }

    public void setJavaObj(int ref, Object v) {
        refs.set(ref, v);
    }

    public void freeJavaObj(int ref) {
        refs.free(ref);
    }

    public int goMalloc(int len) {
        return (int) mallocFn.apply(new long[] {len})[0];
    }

    // returns exitCode
    public int run() {
        ExportFunction startFun = null;
        ExportFunction initializeFun = null;
        for (int i = 0; i < instance.module().exportSection().exportCount(); i++) {
            var export = instance.module().exportSection().getExport(i);
            switch (export.name()) {
                case "_start":
                    startFun = instance.exports().function("_start");
                    break;
                case "_initialize":
                    initializeFun = instance.exports().function("_initialize");
                    break;
            }
        }

        if (startFun != null) {
            // Run program, but catch the wasmExit exception that's thrown
            // to return back here.
            try {
                startFun.apply();
            } catch (WasiExitException wasiExit) {
                return wasiExit.exitCode();
            }
            throw new RuntimeException("unreachable");
        } else {
            initializeFun.apply();
            return 0;
        }
    }

    // low level API: need to have a better typed one on top
    public long[] exec(String export, long[] args) {
        return instance.exports().function(export).apply(args);
    }

    private static ImportFunction[] defaultImports(Go goInstance) {
        return new ImportFunction[] {
            new HostFunction(
                    "env",
                    "allocJava",
                    FunctionType.of(List.of(), List.of(ValType.I32)),
                    (inst, args) -> {
                        return new long[] {goInstance.allocJavaObj(null)};
                    }),
            new HostFunction(
                    "env",
                    "setJavaString",
                    FunctionType.of(List.of(ValType.I32, ValType.I32, ValType.I32), List.of()),
                    (inst, args) -> {
                        int ref = (int) args[0];
                        int sPtr = (int) args[1];
                        int sLen = (int) args[2];

                        var str =
                                new String(
                                        inst.memory().readBytes(sPtr, sLen),
                                        StandardCharsets.UTF_8);

                        goInstance.setJavaObj(ref, str);
                        return null;
                    }),
            new HostFunction(
                    "env",
                    "asGoString",
                    FunctionType.of(List.of(ValType.I32), List.of(ValType.I64)),
                    (inst, args) -> {
                        var ref = (int) args[0];
                        var str = (String) goInstance.getJavaObj(ref);
                        var strBytes = str.getBytes(StandardCharsets.UTF_8);

                        var ptr = goInstance.goMalloc(strBytes.length);
                        inst.memory().write(ptr, strBytes);

                        var resPtr = (((long) ptr) << 32) | (strBytes.length & 0xffffffffL);
                        return new long[] {resPtr};
                    }),
            new HostFunction(
                    "env",
                    "allocJavaBool",
                    FunctionType.of(List.of(ValType.I32), List.of(ValType.I32)),
                    (inst, args) -> {
                        var bool = args[0] > 0;

                        return new long[] {goInstance.allocJavaObj(bool)};
                    }),
            new HostFunction(
                    "env",
                    "setJavaBool",
                    FunctionType.of(List.of(ValType.I32, ValType.I32), List.of()),
                    (inst, args) -> {
                        var ref = (int) args[0];
                        var bool = args[1] > 0;
                        goInstance.setJavaObj(ref, bool);
                        return null;
                    }),
            new HostFunction(
                    "env",
                    "asGoBool",
                    FunctionType.of(List.of(ValType.I32), List.of(ValType.I32)),
                    (inst, args) -> {
                        var ref = (int) args[0];
                        var bool = (Boolean) goInstance.getJavaObj(ref);

                        return (bool) ? new long[] {1} : new long[] {0};
                    }),
            new HostFunction(
                    "env",
                    "free",
                    FunctionType.of(List.of(ValType.I32), List.of()),
                    (inst, args) -> {
                        var ref = (int) args[0];
                        goInstance.freeJavaObj(ref);
                        return null;
                    }),
        };
    }

    public static final class Builder {
        private final WasmModule module;
        private Function<Instance, Machine> machineFactory;
        private ImportFunction[] wasi;
        private Function<Go, ImportFunction[]> additionalImports;
        private boolean defaultImports = true;

        private Builder(WasmModule module) {
            this.module = module;
        }

        public Builder withWasi() {
            return withWasi(WasiOptions.builder().inheritSystem().build());
        }

        public Builder withWasi(WasiOptions wasiOpts) {
            return withWasi(WasiPreview1.builder().withOptions(wasiOpts).build());
        }

        public Builder withWasi(WasiPreview1 wasi) {
            this.wasi = wasi.toHostFunctions();
            return this;
        }

        public Builder withMachineFactory(Function<Instance, Machine> machineFactory) {
            this.machineFactory = machineFactory;
            return this;
        }

        public Builder withNoDefaultImports() {
            this.defaultImports = false;
            return this;
        }

        public Builder withAdditionalImport(Function<Go, ImportFunction[]> importsFun) {
            additionalImports = importsFun;
            return this;
        }

        public Go build() {
            // defaults
            if (machineFactory == null) {
                machineFactory = inst -> new InterpreterMachine(inst);
            }
            if (wasi == null) {
                wasi = new ImportFunction[0];
            }
            if (additionalImports == null) {
                additionalImports = goInst -> new ImportFunction[0];
            }

            return new Go(module, machineFactory, wasi, additionalImports, defaultImports);
        }
    }
}
