package main

import (
	"log"
	"os"
	"time"

	"github.com/getditto/ditto-go-sdk/v5/ditto"
)

// DittoManager owns everything about configuring and running the Ditto
// instance: opening it, wiring up development authentication, and starting
// sync. It knows nothing about tasks.
//
// It vends a configured, already-running Ditto instance (sync is started in
// NewDittoManager) that the rest of the app uses directly via Ditto().
type DittoManager struct {
	ditto   *ditto.Ditto
	tempDir string
}

// NewDittoManager opens a Ditto instance using the Server connection API,
// installs the development-mode authentication handler, and starts sync.
func NewDittoManager(databaseId, token, serverURL, offlineLicenseToken string) (*DittoManager, error) {
	// Create a temp directory for persistence. Using a temporary directory
	// means data is not persistent between runs, but it lets multiple
	// instances run concurrently on the same machine.
	tempDir, err := os.MkdirTemp("", "ditto-quickstart-*")
	if err != nil {
		return nil, err
	}

	var connect ditto.DittoConfigConnect
	if offlineLicenseToken != "" {
		connect = &ditto.DittoConfigConnectSmallPeersOnly{}
	} else {
		connect = &ditto.DittoConfigConnectServer{URL: serverURL}
	}

	// Initialize Ditto with the selected connection API.
	config := ditto.DefaultDittoConfig().
		WithDatabaseID(databaseId).
		WithPersistenceDirectory(tempDir).
		WithConnect(connect)

	d, err := ditto.Open(config)
	if err != nil {
		os.RemoveAll(tempDir)
		return nil, err
	}

	if offlineLicenseToken != "" {
		if err := d.SetOfflineOnlyLicenseToken(offlineLicenseToken); err != nil {
			d.Close()
			os.RemoveAll(tempDir)
			return nil, err
		}
	} else if auth := d.Auth(); auth != nil {
		auth.SetExpirationHandler(
			func(d *ditto.Ditto, timeUntilExpiration time.Duration) {
				log.Printf("Expiration handler called with time until expiration: %v", timeUntilExpiration)

				// For development mode, login with the development token
				provider := ditto.DevelopmentAuthenticationProvider()
				clientInfoJSON, err := d.Auth().Login(token, provider)
				if err != nil {
					log.Printf("Failed to login: %v", err)
				} else {
					log.Printf("Login successful")
					if clientInfoJSON != "" {
						log.Printf("Client info: %s", clientInfoJSON)
					}
				}
			})
	}

	// Start sync (authentication handler will be called automatically if needed)
	if err := d.Sync().Start(); err != nil {
		d.Close()
		os.RemoveAll(tempDir)
		return nil, err
	}

	return &DittoManager{ditto: d, tempDir: tempDir}, nil
}

// Ditto exposes the running Ditto instance so callers can use the real Ditto
// API directly.
func (m *DittoManager) Ditto() *ditto.Ditto {
	return m.ditto
}

// Close shuts down the Ditto instance and removes the persistence directory.
func (m *DittoManager) Close() {
	m.ditto.Close()
	os.RemoveAll(m.tempDir)
}
