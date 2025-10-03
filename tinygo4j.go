package tinygo4j

import (
	"unsafe"
)

// #include <stdlib.h>
import "C"

type JavaRef uint32
type SetBuilder struct {
	ref JavaRef
}

func Alloc() JavaRef {
	return allocJava()
}

func (ref JavaRef) Set() SetBuilder {
	return SetBuilder{ref}
}

func Set(ref JavaRef) SetBuilder {
	return SetBuilder{ref}
}

func (set SetBuilder) String(str string) JavaRef {
	setJavaString(set.ref, str)
	return set.ref
}

func (set SetBuilder) Bool(v bool) JavaRef {
	setJavaBool(set.ref, v)
	return set.ref
}

func (ref JavaRef) AsString() string {
	v := asGoString(ref)
	ptr := unsafe.Pointer(uintptr(uint32(v >> 32)))
	length := int(uint32(v))

	if ptr == nil || length == 0 {
		return ""
	}

	buffer := append([]byte(nil), unsafe.Slice((*byte)(ptr), length)...)
	result := string(buffer)

	C.free(ptr)
	return result
}

func (ref JavaRef) AsBool() bool {
	return asGoBool(ref)
}

func (ref JavaRef) Free() {
	free(ref)
}

//go:wasmimport env allocJava
func allocJava() JavaRef

//go:wasmimport env setJavaString
func setJavaString(ref JavaRef, str string)

//go:wasmimport env asGoString
func asGoString(str JavaRef) uint64

//go:wasmimport env setJavaBool
func setJavaBool(ref JavaRef, v bool)

//go:wasmimport env asGoBool
func asGoBool(ref JavaRef) bool

//go:wasmimport env free
func free(str JavaRef)
