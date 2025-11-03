package main

import "github.com/gizak/termui/v3/widgets"

// BorderlessParagraph creates a paragraph without borders and removes the default inner padding.
// This allows safely rendering a paragraph that is smaller than three rows or three columns.
// The default calculation of the Inner rectangle in termui.Block#SetRect will produce an invalid rectangle when the
// height or width of the passed coordinates is two or less. Negating the introduced offsets with negative
// values for Padding allows the text to render as expected.
func BorderlessParagraph() *widgets.Paragraph {
	p := widgets.NewParagraph()
	p.Border = false
	p.PaddingLeft = -1
	p.PaddingTop = -1
	p.PaddingRight = -1
	p.PaddingBottom = -1
	return p
}
