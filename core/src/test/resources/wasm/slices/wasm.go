package main

import (
	"fmt"
	"github.com/roastedroot/go4j"
	"os"
	"strings"
)

func splitter(strRef go4j.JavaRef) go4j.JavaRef {
	str := strRef.AsString()
	values := strings.Split(str, ",")

	result := make([]interface{}, 0)
	for _, each := range values {
		result = append(result, each)
	}

	return go4j.Alloc().Set().String(fmt.Sprintf("%v", result))
}

func main() {
	arg1 := os.Args[1]
	in := go4j.Alloc().Set().String(arg1)
	str := splitter(in).AsString()
	println(str)
}
