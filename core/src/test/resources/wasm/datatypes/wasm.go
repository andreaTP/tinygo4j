package main

import (
	"github.com/roastedroot/tinygo4j"
)

//go:wasmexport roundtripString
func roundtripString(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	str := in.AsString()
	return tinygo4j.Alloc().Set().String(str)
}

//go:wasmexport roundtripBytes
func roundtripBytes(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	bytes := in.AsBytes()
	return tinygo4j.Alloc().Set().Bytes(bytes)
}

//go:wasmexport roundtripUint32
func roundtripUint32(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsUint32()
	return tinygo4j.Alloc().Set().Int(v)
}

//go:wasmexport roundtripUint64
func roundtripUint64(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsUint64()
	return tinygo4j.Alloc().Set().Long(v)
}

//go:wasmexport roundtripFloat32
func roundtripFloat32(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsFloat32()
	return tinygo4j.Alloc().Set().Float(v)
}

//go:wasmexport roundtripFloat64
func roundtripFloat64(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsFloat64()
	return tinygo4j.Alloc().Set().Double(v)
}

//go:wasmexport roundtripBool
func roundtripBool(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsBool()
	return tinygo4j.Alloc().Set().Bool(v)
}

func main() {}




