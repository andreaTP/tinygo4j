package main

import (
	"github.com/roastedroot/tinygo4j"
)

//go:wasmimport from_java my_java_func
func myJavaFunc(x, y uint32) tinygo4j.JavaRef

//go:wasmimport from_java my_java_check
func myJavaCheck(value tinygo4j.JavaRef)

//go:wasmimport from_java my_java_ref
func myJavaRef() tinygo4j.JavaRef

//go:wasmimport from_java my_java_ref_check
func myJavaRefCheck(value tinygo4j.JavaRef)

//export test1
func test1() {
	myJavaCheck(myJavaFunc(40, 2))
}

//export test2
func test2() {
	myJavaRefCheck(myJavaRef());
}

func main() {
}
