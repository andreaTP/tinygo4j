package main

//go:wasmexport operation
func operation(i uint32) uint32 {
	return i + 2
}

func main() {
}
