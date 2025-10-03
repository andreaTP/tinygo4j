package tinygo4j

import (
	"unsafe"
)

type JavaRef uint32
type Builder struct {}

func Alloc() Builder {
	return Builder{}
}

func (Builder) String(str string) JavaRef {
	return allocJavaString(str)
}

func (Builder) Bool(v bool) JavaRef {
	return allocJavaBool(v)
}

func (ref JavaRef) AsString() string {
	v := asGoString(ref)
	ptr := uint32(v >> 4)
	len := uint32(v)

	// TODO: improve the implementation
	buffer := make([]byte, len)
	for i := 0; i < int(len); i++ {
		s := *(*int32)(unsafe.Pointer(uintptr(ptr) + uintptr(i)))
		buffer[i] = byte(s)
	}
	return string(buffer)
}

func (ref JavaRef) AsBool() bool {
	return asGoBool(ref)
}


func (ref JavaRef) Free() {
	free(ref)
}

//go:wasmimport env allocJavaString
func allocJavaString(str string) JavaRef

//go:wasmimport env asGoString
func asGoString(str JavaRef) uint64

//go:wasmimport env allocJavaBool
func allocJavaBool(v bool) JavaRef

//go:wasmimport env asGoBool
func asGoBool(ref JavaRef) bool

//go:wasmimport env free
func free(str JavaRef)

