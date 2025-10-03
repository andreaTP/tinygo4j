package main

import (
	"github.com/andreatp/tinygo4j"
)

//export usage
func usage() uint32 {
    dyn := tinygo4j.ValueNew()
    tinygo4j.ValueSet(dyn, "foo", 987)
    return tinygo4j.ValueGet(dyn, "foo")
}

func main() {
}
