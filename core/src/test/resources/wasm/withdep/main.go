package main

import (
	qrcode "github.com/skip2/go-qrcode"
	"github.com/roastedroot/tinygo4j"
)

//export genqr
func genqr(strRef tinygo4j.JavaRef) tinygo4j.JavaRef {
	str := strRef.AsString()

	var png []byte
  	png, _ = qrcode.Encode(str, qrcode.Medium, 256)

	return tinygo4j.Alloc().Set().Bytes(png)
}

func main() {
}
