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

	ticker := time.NewTicker(200 * time.Millisecond)
	quit := make(chan struct{})
	go func() {
		for {
		select {
			case <- ticker.C:
				if update(strRef) {
					close(quit)
				}
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

func update(strRef tinygo4j.JavaRef) bool {
	str, strPtr := strRef.AsString()

	n, _ := strconv.Atoi(str)

	if (n > 10) {
		reset(strRef)
		C.free(strPtr)
		return true
	} else {
		tinygo4j.Set(strRef).String(strconv.Itoa(n + 1))
	}

	C.free(strPtr)
	return false
}
