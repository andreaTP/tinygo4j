package main

import (
	"github.com/roastedroot/go4j"
)

//go:wasmimport mygo javaValidate
func myJavaValidate(str go4j.JavaRef) go4j.JavaRef

//go:wasmexport usage
func usage(strRef go4j.JavaRef) bool {
	str := strRef.AsString()
	strRef2 := go4j.Alloc().Set().String(str)

	valid := myJavaValidate(strRef2)

	strRef2.Free()

	return valid.AsBool()
}

func main() {
}
