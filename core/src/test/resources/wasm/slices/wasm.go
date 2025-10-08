package main

import (
	"fmt"
	"github.com/roastedroot/tinygo4j"
	"os"
	"strings"
)

func splitter(strRef tinygo4j.JavaRef) tinygo4j.JavaRef {
	str := strRef.AsString()
	values := strings.Split(str, ",")

	result := make([]interface{}, 0)
	for _, each := range values {
		result = append(result, each)
	}

	return tinygo4j.Alloc().Set().String(fmt.Sprintf("%v", result))
}

func main() {
	arg1 := os.Args[1]
	in := tinygo4j.Alloc().Set().String(arg1)
	str := splitter(in).AsString()
	println(str)
}
