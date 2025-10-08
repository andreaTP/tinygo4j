package main

import (
	"github.com/roastedroot/tinygo4j"
)

//export roundtripString
func roundtripString(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	str := in.AsString()
	return tinygo4j.Alloc().Set().String(str)
}

//export roundtripBytes
func roundtripBytes(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	bytes := in.AsBytes()
	return tinygo4j.Alloc().Set().Bytes(bytes)
}

//export roundtripUint32
func roundtripUint32(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsUint32()
	return tinygo4j.Alloc().Set().Int(v)
}

//export roundtripUint64
func roundtripUint64(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsUint64()
	return tinygo4j.Alloc().Set().Long(v)
}

//export roundtripFloat32
func roundtripFloat32(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsFloat32()
	return tinygo4j.Alloc().Set().Float(v)
}

//export roundtripFloat64
func roundtripFloat64(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsFloat64()
	return tinygo4j.Alloc().Set().Double(v)
}

//export roundtripBool
func roundtripBool(in tinygo4j.JavaRef) tinygo4j.JavaRef {
	v := in.AsBool()
	return tinygo4j.Alloc().Set().Bool(v)
}

func main() {}




