package main

import (
	"github.com/roastedroot/go4j"
)

//go:wasmexport roundtripString
func roundtripString(in go4j.JavaRef) go4j.JavaRef {
	str := in.AsString()
	return go4j.Alloc().Set().String(str)
}

//go:wasmexport roundtripBytes
func roundtripBytes(in go4j.JavaRef) go4j.JavaRef {
	bytes := in.AsBytes()
	return go4j.Alloc().Set().Bytes(bytes)
}

//go:wasmexport roundtripUint32
func roundtripUint32(in go4j.JavaRef) go4j.JavaRef {
	v := in.AsUint32()
	return go4j.Alloc().Set().Int(v)
}

//go:wasmexport roundtripUint64
func roundtripUint64(in go4j.JavaRef) go4j.JavaRef {
	v := in.AsUint64()
	return go4j.Alloc().Set().Long(v)
}

//go:wasmexport roundtripFloat32
func roundtripFloat32(in go4j.JavaRef) go4j.JavaRef {
	v := in.AsFloat32()
	return go4j.Alloc().Set().Float(v)
}

//go:wasmexport roundtripFloat64
func roundtripFloat64(in go4j.JavaRef) go4j.JavaRef {
	v := in.AsFloat64()
	return go4j.Alloc().Set().Double(v)
}

//go:wasmexport roundtripBool
func roundtripBool(in go4j.JavaRef) go4j.JavaRef {
	v := in.AsBool()
	return go4j.Alloc().Set().Bool(v)
}

func main() {}




