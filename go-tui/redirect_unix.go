//go:build !windows
// +build !windows

package main

import (
	"os"
	"syscall"

	"golang.org/x/term"
)

// isTerminal checks if the given file descriptor is a terminal
func isTerminal(fd uintptr) bool {
	return term.IsTerminal(int(fd))
}

// redirectStderr redirects stderr to /dev/null on Unix systems
func redirectStderr() {
	// Redirect stderr to /dev/null so it doesn't interfere with TUI output
	// This is similar to what the C++ TUI does with freopen
	if isTerminal(os.Stderr.Fd()) {
		devNull, err := os.OpenFile(os.DevNull, os.O_WRONLY, 0)
		if err == nil {
			// Redirect stderr to /dev/null
			syscall.Dup2(int(devNull.Fd()), int(os.Stderr.Fd()))
			devNull.Close()
		}
	}
}
