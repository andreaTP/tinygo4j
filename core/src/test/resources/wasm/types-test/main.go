package main

import (
	"github.com/roastedroot/tinygo4j"
)

// Host functions for primitive types as parameters
//go:wasmimport from_java test_int_param
func testIntParam(value int32)

//go:wasmimport from_java test_long_param
func testLongParam(value int64)

//go:wasmimport from_java test_double_param
func testDoubleParam(value float64)

//go:wasmimport from_java test_float_param
func testFloatParam(value float32)

//go:wasmimport from_java test_bool_param
func testBoolParam(value bool)

// Host functions for primitive types as returns
//go:wasmimport from_java test_int_return
func testIntReturn() int32

//go:wasmimport from_java test_long_return
func testLongReturn() int64

//go:wasmimport from_java test_double_return
func testDoubleReturn() float64

//go:wasmimport from_java test_float_return
func testFloatReturn() float32

//go:wasmimport from_java test_bool_return
func testBoolReturn() bool

// Host functions for references
//go:wasmimport from_java test_ref_param
func testRefParam(value tinygo4j.JavaRef)

//go:wasmimport from_java test_ref_return
func testRefReturn() tinygo4j.JavaRef

//go:wasmimport from_java test_ref_check
func testRefCheck(value tinygo4j.JavaRef)

// Guest functions to test primitive types passing from Go to Java
//export test_int_to_java
func testIntToJava() {
	testIntParam(42)
}

//export test_long_to_java
func testLongToJava() {
	testLongParam(9223372036854775807) // max int64
}

//export test_double_to_java
func testDoubleToJava() {
	testDoubleParam(3.141592653589793)
}

//export test_float_to_java
func testFloatToJava() {
	testFloatParam(2.718281828459045)
}

//export test_bool_to_java
func testBoolToJava() {
	testBoolParam(true)
}

// Guest functions to test primitive types returning from Java to Go
//export test_int_from_java
func testIntFromJava() {
	result := testIntReturn()
	testIntParam(result + 1) // Verify we can use the returned value
}

//export test_long_from_java
func testLongFromJava() {
	result := testLongReturn()
	testLongParam(result - 1) // Verify we can use the returned value
}

//export test_double_from_java
func testDoubleFromJava() {
	result := testDoubleReturn()
	testDoubleParam(result * 2.0) // Verify we can use the returned value
}

//export test_float_from_java
func testFloatFromJava() {
	result := testFloatReturn()
	testFloatParam(result / 2.0) // Verify we can use the returned value
}

//export test_bool_from_java
func testBoolFromJava() {
	result := testBoolReturn()
	testBoolParam(!result) // Verify we can use the returned value
}

// Guest functions to test reference types
//export test_ref_to_java
func testRefToJava() {
	// Create a reference and pass it to Java
	ref := testRefReturn()
	testRefParam(ref)
}

//export test_ref_from_java
func testRefFromJava() {
	// Get a reference from Java and verify it
	ref := testRefReturn()
	testRefCheck(ref)
}

//export test_ref_roundtrip
func testRefRoundtrip() {
	// Test roundtrip: Go -> Java -> Go
	ref := testRefReturn()
	testRefParam(ref)
	// Java should call back with the same reference
}

// Guest functions with parameters and return values
//export test_int_param_return
func testIntParamReturn(value int32) int32 {
	return value + 1
}

//export test_long_param_return
func testLongParamReturn(value int64) int64 {
	return value - 1
}

//export test_double_param_return
func testDoubleParamReturn(value float64) float64 {
	return value * 2.0
}

//export test_float_param_return
func testFloatParamReturn(value float32) float32 {
	return value / 2.0
}

//export test_bool_param_return
func testBoolParamReturn(value bool) bool {
	return !value
}

//export test_ref_param_return
func testRefParamReturn(value tinygo4j.JavaRef) tinygo4j.JavaRef {
	return value
}

// Guest functions with multiple parameters
//export test_multi_int_params
func testMultiIntParams(a, b int32) int32 {
	return a + b
}

//export test_multi_long_params
func testMultiLongParams(a, b int64) int64 {
	return a - b
}

//export test_multi_double_params
func testMultiDoubleParams(a, b float64) float64 {
	return a * b
}

//export test_multi_float_params
func testMultiFloatParams(a, b float32) float32 {
	return a / b
}

//export test_multi_bool_params
func testMultiBoolParams(a, b bool) bool {
	return a && b
}

//export test_mixed_params
func testMixedParams(i int32, l int64, d float64, f float32, b bool) int32 {
	if b {
		return i + int32(l) + int32(d) + int32(f)
	}
	return 0
}

func main() {
}
