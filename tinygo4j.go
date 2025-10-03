package tinygo4j

import (
	"unsafe"
)

type JavaRef uint32
type AllocBuilder struct {}
type SetBuilder struct {
	ref JavaRef
}

func Alloc() AllocBuilder {
	return AllocBuilder{}
}

func (AllocBuilder) String(str string) JavaRef {
	return allocJavaString(str)
}

func (AllocBuilder) Bool(v bool) JavaRef {
	return allocJavaBool(v)
}

func Set(ref JavaRef) SetBuilder {
	return SetBuilder{ref}
}

func (set SetBuilder) String(str string) {
	setJavaString(set.ref, str)
}

func (set SetBuilder) Bool(v bool) {
	setJavaBool(set.ref, v)
}

func (ref JavaRef) AsString() (string, unsafe.Pointer) {
    v := asGoString(ref)
    ptr := unsafe.Pointer(uintptr(uint32(v >> 4)))
    length := int(uint32(v))

    if ptr == nil || length == 0 {
        return "", nil
    }

    buffer := append([]byte(nil), unsafe.Slice((*byte)(ptr), length)...)
    return string(buffer), ptr
}

func (ref JavaRef) AsBool() bool {
	return asGoBool(ref)
}


func (ref JavaRef) Free() {
	free(ref)
}

//go:wasmimport env allocJavaString
func allocJavaString(str string) JavaRef

//go:wasmimport env setJavaString
func setJavaString(ref JavaRef, str string)

//go:wasmimport env asGoString
func asGoString(str JavaRef) uint64

//go:wasmimport env allocJavaBool
func allocJavaBool(v bool) JavaRef

//go:wasmimport env setJavaBool
func setJavaBool(ref JavaRef, v bool)

//go:wasmimport env asGoBool
func asGoBool(ref JavaRef) bool

//go:wasmimport env free
func free(str JavaRef)

