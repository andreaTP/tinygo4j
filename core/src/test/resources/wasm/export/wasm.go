package main

import (
	"github.com/roastedroot/tinygo4j"
	"strconv"
)

func main() {
}

//go:wasmexport add
func add(a, b uint32) uint32 {
	return a + b
}

//go:wasmexport update
func update(aRef, bRef tinygo4j.JavaRef) tinygo4j.JavaRef {
	aStr := aRef.AsString()
	bStr := bRef.AsString()
	a, _ := strconv.Atoi(aStr)
	b, _ := strconv.Atoi(bStr)
	result := add(uint32(a), uint32(b))

	return tinygo4j.Alloc().Set().String(strconv.Itoa(int(result)))
}
