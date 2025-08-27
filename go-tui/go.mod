module github.com/getditto/quickstart/go-tui/ditto-tasks-termui

go 1.24

toolchain go1.24.5

require (
	github.com/getditto/ditto-go-sdk v0.0.0
	github.com/gizak/termui/v3 v3.1.0
	github.com/google/uuid v1.6.0
	github.com/joho/godotenv v1.5.1
	golang.org/x/term v0.34.0
)

// TODO(go): remove this line when the Go SDK is published to the web
replace github.com/getditto/ditto-go-sdk => ../../ditto/sdks/go

require (
	github.com/fxamacker/cbor/v2 v2.5.0 // indirect
	github.com/mattn/go-runewidth v0.0.16 // indirect
	github.com/mitchellh/go-wordwrap v0.0.0-20150314170334-ad45545899c7 // indirect
	github.com/nsf/termbox-go v0.0.0-20190121233118-02980233997d // indirect
	github.com/rivo/uniseg v0.4.7 // indirect
	github.com/x448/float16 v0.8.4 // indirect
	golang.org/x/sys v0.35.0 // indirect
)
