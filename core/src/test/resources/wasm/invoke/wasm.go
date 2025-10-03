package main

import (
	"github.com/andreatp/tinygo4j"
)

func runner(this tinygo4j.Value, args []tinygo4j.Value) interface{} {
	return args[0].Invoke(args[1]).String()
}

func main() {
	wait := make(chan struct{}, 0)
	js.Global().Set("runner", js.FuncOf(runner))
	<-wait
}
