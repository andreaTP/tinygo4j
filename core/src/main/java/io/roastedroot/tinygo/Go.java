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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Go {
    private final Instance instance;
    private final RefStore refs = new RefStore();

    private Go(WasmModule module,
               Function<Instance, Machine> machineFactory,
               ImportFunction[] wasi,
               ImportFunction[] additionalImports,
               boolean defaultImports) {
        this.instance = Instance.builder(module)
                .withImportValues(ImportValues.builder()
                        .addFunction(wasi)
                        .addFunction(additionalImports)
                        .addFunction((defaultImports) ? defaultImports(this) : new ImportFunction[0])
                        .build())
                .withMachineFactory(machineFactory)
                .withMemoryFactory(ByteArrayMemory::new)
                .withStart(false)
                .build();
    }

    public static Builder builder(WasmModule module) {
        return new Builder(module);
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

    private String loadString(int ptr, int len) {
        return new String(instance.memory().readBytes(ptr, len), StandardCharsets.UTF_8);
    }

    private static ImportFunction[] defaultImports(Go goInstance) {
        return new ImportFunction[]{
                new HostFunction("env", "valueNew",
                        FunctionType.of(
                                List.of(),
                                List.of(ValType.I32)
                        ),
                        (inst, args) -> {
                            return new long[] { goInstance.refs.registerRef(new HashMap<String, Object>()) };
                        }
                ),
                new HostFunction("env", "valueSet",
                        FunctionType.of(
                                List.of(ValType.I32, ValType.I32, ValType.I32, ValType.I32),
                                List.of()
                        ),
                        (inst, args) -> {
                            int vRef = (int) args[0];
                            int pPtr = (int) args[1];
                            int pLen = (int) args[2];
                            long value = args[3];

                            var ref = goInstance.refs.get(vRef);
                            if (ref instanceof Map) {
                                ((Map<String, Object>) ref).put(goInstance.loadString(pPtr, pLen), value);
                            } else {
                                throw new RuntimeException("unsupported valueSet type " + ref);
                            }
                            return null;
                        }
                ),
                new HostFunction("env", "valueGet",
                        FunctionType.of(
                                List.of(ValType.I32, ValType.I32, ValType.I32),
                                List.of(ValType.I32)
                        ),
                        (inst, args) -> {
                            int vRef = (int) args[0];
                            int pPtr = (int) args[1];
                            int pLen = (int) args[2];

                            var ref = goInstance.refs.get(vRef);
                            if (ref instanceof Map) {
                                return new long[] {(long) ((Map<String, Object>) ref).get(goInstance.loadString(pPtr, pLen))};
                            } else {
                                throw new RuntimeException("unsupported valueSet type " + ref);
                            }
                        }
                )
        };
    }

    public static class Builder {
        private final WasmModule module;
        private Function<Instance, Machine> machineFactory;
        private ImportFunction[] wasi;
        private ImportFunction[] additionalImports;
        private boolean defaultImports = true;

        private Builder(WasmModule module) {
            this.module = module;
        }

        public Builder withWasi() {
            return withWasi(WasiOptions.builder().build());
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

        public Builder withAdditionalImport(ImportFunction... imports) {
            if (additionalImports == null) {
                additionalImports = imports;
            } else {
                var length = additionalImports.length;
                additionalImports = Arrays.copyOf(additionalImports, length + imports.length);
                System.arraycopy(imports, 0, additionalImports, length, imports.length);
            }
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
                additionalImports = new ImportFunction[0];
            }

            return new Go(module, machineFactory, wasi, additionalImports, defaultImports);
        }
    }
}
