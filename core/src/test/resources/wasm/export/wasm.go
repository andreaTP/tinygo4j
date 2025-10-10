package main

import (
	"github.com/roastedroot/go4j"
	"strconv"
)

func main() {
}

//go:wasmexport add
func add(a, b uint32) uint32 {
	return a + b
}

//go:wasmexport update
func update(aRef, bRef go4j.JavaRef) go4j.JavaRef {
	aStr := aRef.AsString()
	bStr := bRef.AsString()
	a, _ := strconv.Atoi(aStr)
	b, _ := strconv.Atoi(bStr)
	result := add(uint32(a), uint32(b))

	return go4j.Alloc().Set().String(strconv.Itoa(int(result)))
}
