package main

import (
	"time"
	"strconv"
	"github.com/andreatp/tinygo4j"
)

// #include <stdlib.h>
import "C"

func main() {
	strRef := tinygo4j.Alloc().String("0")

	ticker := time.NewTicker(1 * time.Second)
	quit := make(chan struct{})
	go func() {
		for {
		select {
			case <- ticker.C:
				update(strRef)
			case <- quit:
				ticker.Stop()
				return
			}
		}
	}()
	<-quit
}

//go:wasmimport mygo reset
func reset(strRef tinygo4j.JavaRef)

func update(strRef tinygo4j.JavaRef) {
	str, strPtr := strRef.AsString()

	println(str)

	n, _ := strconv.Atoi(str)

	if (n > 10) {
		reset(strRef)
	} else {
		tinygo4j.Set(strRef).String(strconv.Itoa(n + 1))
	}

	C.free(strPtr)
}
