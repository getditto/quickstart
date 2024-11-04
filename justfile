#
# justfile
#

_default:
    @just --list

# os_family()
cmdline_url_linux := "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
cmdline_url_mac := "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
cmdline_tools_dir := env_var('ANDROID_HOME') + "/cmdline-tools"

# Installs the Android command line tools
[macos]
cmdline_tools:
    #!/usr/bin/env bash
    set -euo pipefail

    if [[ ! -d {{cmdline_tools_dir}} ]]; then
        echo "Installing Android command line tools"
        mkdir -p {{cmdline_tools_dir}}
        curl --fail --no-progress-meter --location {{cmdline_url_mac}} |
            tar --extract --directory {{cmdline_tools_dir}}
        mv -v {{cmdline_tools_dir}}/cmdline-tools/  {{cmdline_tools_dir}}/latest/
        for file in {{cmdline_tools_dir}}/latest/bin/*; do
            chmod +x ${file}
        done
        echo "  ✅ installed to: {{cmdline_tools_dir}}"
    else
        echo "The Android command line tools are already installed at {{cmdline_tools_dir}}"
    fi

# Installs the Android command line tools
[linux]
cmdline_tools:
    #!/usr/bin/env bash
    set -euo pipefail

    if [[ ! -d {{cmdline_tools_dir}} ]]; then
        echo "Installing Android command line tools"
        mkdir -p {{cmdline_tools_dir}}
        curl --fail --no-progress-meter --location {{cmdline_url_linux}} |
            tar --extract --directory {{cmdline_tools_dir}}
        mv -v {{cmdline_tools_dir}}/cmdline-tools/  {{cmdline_tools_dir}}/latest/
        for file in {{cmdline_tools_dir}}/latest/bin/*; do
            chmod +x ${file}
        done
        echo "  ✅ installed to: {{cmdline_tools_dir}}"
    else
        echo "The Android command line tools are already installed at {{cmdline_tools_dir}}"
    fi

# Installs required tools
tools: cmdline_tools
    @echo "Installing tools"
    {{cmdline_tools_dir}}/latest/bin/sdkmanager --install 'build-tools;35.0.0'
    {{cmdline_tools_dir}}/latest/bin/sdkmanager --install 'platforms;android-34'
