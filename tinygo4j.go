package tinygo4j

type ref = uint32

func ValueNew() ref {
	return envValueNew()
}

// TODO: verify is multiple dispatch works
func ValueSet(v ref, name string, value ref) {
	envValueSet(v, name, value)
}

func ValueGet(v ref, name string) ref {
	return envValueGet(v, name)
}

//go:wasmimport env valueNew
func envValueNew() ref

//go:wasmimport env valueSet
func envValueSet(v ref, name string, value ref)

//go:wasmimport env valueGet
func envValueGet(v ref, name string) ref
