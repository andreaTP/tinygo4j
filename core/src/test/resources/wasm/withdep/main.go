package main

import (
	qrcode "github.com/skip2/go-qrcode"
	"github.com/roastedroot/go4j"
)

//go:wasmexport genqr
func genqr(strRef go4j.JavaRef) go4j.JavaRef {
	str := strRef.AsString()

	var png []byte
  	png, _ = qrcode.Encode(str, qrcode.Medium, 256)

	return go4j.Alloc().Set().Bytes(png)
}

func main() {
}
