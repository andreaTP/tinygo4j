package main

import (
	"github.com/roastedroot/go4j"
)

//go:wasmimport from_java my_java_func
func myJavaFunc(x, y uint32) go4j.JavaRef

//go:wasmimport from_java my_java_check
func myJavaCheck(value go4j.JavaRef)

//go:wasmimport from_java my_java_ref
func myJavaRef() go4j.JavaRef

//go:wasmimport from_java my_java_ref_check
func myJavaRefCheck(value go4j.JavaRef)

//go:wasmexport test1
func test1() {
	myJavaCheck(myJavaFunc(40, 2))
}

//go:wasmexport test2
func test2() {
	myJavaRefCheck(myJavaRef());
}

func main() {
}
