//go:build windows
// +build windows

package main

import (
	"golang.org/x/term"
)

// isTerminal checks if the given file descriptor is a terminal
func isTerminal(fd uintptr) bool {
	return term.IsTerminal(int(fd))
}

// redirectStderr is a no-op on Windows for now
// TODO: Implement Windows-specific stderr redirection if needed
func redirectStderr() {
	// On Windows, we don't redirect stderr for now
	// The terminal UI might still work, or we could implement
	// Windows-specific redirection using Windows API calls
}
