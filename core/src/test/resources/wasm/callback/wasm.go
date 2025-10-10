package main

import (
	"github.com/roastedroot/go4j"
	"strconv"
	"time"
)

func main() {
	strRef := go4j.Alloc().Set().String("0")

	ticker := time.NewTicker(200 * time.Millisecond)
	quit := make(chan struct{})
	go func() {
		for {
			select {
			case <-ticker.C:
				if update(strRef) {
					close(quit)
				}
			case <-quit:
				ticker.Stop()
				return
			}
		}
	}()
	<-quit
}

//go:wasmimport mygo reset
func reset(strRef go4j.JavaRef)

func update(strRef go4j.JavaRef) bool {
	str := strRef.AsString()

	n, _ := strconv.Atoi(str)

	if n > 10 {
		reset(strRef)
		return true
	} else {
		go4j.Set(strRef).String(strconv.Itoa(n + 1))
	}

	return false
}
