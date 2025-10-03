package main

import (
	"strconv"
	"github.com/andreatp/tinygo4j"
)

// #include <stdlib.h>
import "C"

func main() {
}

//export add
func add(a, b int) int {
	return a + b
}

//export update
func update(aRef, bRef tinygo4j.JavaRef) tinygo4j.JavaRef {
	aStr, aPtr := aRef.AsString()
	bStr, bPtr := bRef.AsString()
	a, _ := strconv.Atoi(aStr)
	b, _ := strconv.Atoi(bStr)
	result := add(a, b)

	C.free(aPtr)
	C.free(bPtr)

	return tinygo4j.Alloc().String(strconv.Itoa(result))
}
