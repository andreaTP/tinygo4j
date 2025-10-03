package main

import (
	"github.com/andreatp/tinygo4j"
)

//go:wasmimport mygo javaValidate
func myJavaValidate(str tinygo4j.JavaRef) tinygo4j.JavaRef

//export usage
func usage(str tinygo4j.JavaRef) bool {
    strRef := tinygo4j.Alloc().String(str.AsString())

    valid := myJavaValidate(strRef)

    strRef.Free()

    return valid.AsBool()
}

func main() {
}
