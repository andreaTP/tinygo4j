package main

import (
	"github.com/roastedroot/tinygo4j"
)

//go:wasmimport mygo javaValidate
func myJavaValidate(str tinygo4j.JavaRef) tinygo4j.JavaRef

//go:wasmexport usage
func usage(strRef tinygo4j.JavaRef) bool {
	str := strRef.AsString()
	strRef2 := tinygo4j.Alloc().Set().String(str)

	valid := myJavaValidate(strRef2)

	strRef2.Free()

	return valid.AsBool()
}

func main() {
}
