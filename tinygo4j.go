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

func (set SetBuilder) Bytes(bytes []byte) JavaRef {
	setJavaBytes(set.ref, unsafe.Pointer(&bytes[0]), uint32(len(bytes)))
	return set.ref
}

func (set SetBuilder) Int(v uint32) JavaRef {
	setJavaInt(set.ref, v)
	return set.ref
}

func (set SetBuilder) Long(v uint64) JavaRef {
	setJavaLong(set.ref, v)
	return set.ref
}

func (set SetBuilder) Float(v float32) JavaRef {
	setJavaFloat(set.ref, v)
	return set.ref
}

func (set SetBuilder) Double(v float64) JavaRef {
	setJavaDouble(set.ref, v)
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

func (ref JavaRef) AsBytes() []byte {
	v := asGoBytes(ref)
	ptr := unsafe.Pointer(uintptr(uint32(v >> 32)))
	length := int(uint32(v))

	if ptr == nil || length == 0 {
		return []byte{}
	}

	buffer := append([]byte(nil), unsafe.Slice((*byte)(ptr), length)...)

	C.free(ptr)
	return buffer
}

func (ref JavaRef) AsUint32() uint32 {
	return asGoUint32(ref)
}

func (ref JavaRef) AsUint64() uint64 {
	return asGoUint64(ref)
}

func (ref JavaRef) AsFloat32() float32 {
	return asGoFloat32(ref)
}

func (ref JavaRef) AsFloat64() float64 {
	return asGoFloat64(ref)
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

//go:wasmimport env setJavaBytes
func setJavaBytes(ref JavaRef, bytesPtr unsafe.Pointer, len uint32)

//go:wasmimport env setJavaInt
func setJavaInt(ref JavaRef, v uint32)

//go:wasmimport env setJavaLong
func setJavaLong(ref JavaRef, v uint64)

//go:wasmimport env setJavaFloat
func setJavaFloat(ref JavaRef, v float32)

//go:wasmimport env setJavaDouble
func setJavaDouble(ref JavaRef, v float64)

//go:wasmimport env setJavaBool
func setJavaBool(ref JavaRef, v bool)

//go:wasmimport env asGoString
func asGoString(str JavaRef) uint64

//go:wasmimport env asGoBytes
func asGoBytes(str JavaRef) uint64

//go:wasmimport env asGoUint32
func asGoUint32(ref JavaRef) uint32

//go:wasmimport env asGoUint64
func asGoUint64(ref JavaRef) uint64

//go:wasmimport env asGoFloat32
func asGoFloat32(ref JavaRef) float32

//go:wasmimport env asGoFloat64
func asGoFloat64(ref JavaRef) float64

//go:wasmimport env asGoBool
func asGoBool(ref JavaRef) bool

//go:wasmimport env free
func free(str JavaRef)
