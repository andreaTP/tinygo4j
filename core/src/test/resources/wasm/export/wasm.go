package main

import (
	"strconv"
	"github.com/andreatp/tinygo4j"
)

func main() {
}

//export add
func add(a, b int) int {
	return a + b
}

//export update
func update(aRef, bRef tinygo4j.JavaRef) tinygo4j.JavaRef {
	aStr := aRef.AsString()
	bStr := bRef.AsString()
	a, _ := strconv.Atoi(aStr)
	b, _ := strconv.Atoi(bStr)
	result := add(a, b)

	return tinygo4j.Alloc().Set().String(strconv.Itoa(result))
}
