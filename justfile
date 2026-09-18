#
# justfile
#

_default:
    @just --list

platform := if os_family() == "macos" { "mac" } else { "linux" }
cmdline_url := "https://dl.google.com/android/repository/commandlinetools-{{ platform }}-11076708_latest.zip"
cmdline_tools_dir := env_var('ANDROID_HOME') + "/cmdline-tools"

# Installs the Android command line tools
cmdline_tools:
    #!/usr/bin/env bash
    set -euo pipefail

    if [[ ! -d {{cmdline_tools_dir}} ]]; then
        echo "Installing Android command line tools"
        mkdir -p {{cmdline_tools_dir}}
        curl --fail --no-progress-meter --location {{cmdline_url}} |
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

# Runs the Python TUI quickstart
python-tui:
    #!/usr/bin/env bash
    set -euo pipefail

    cd python-tui
    if command -v uv >/dev/null 2>&1; then
        uv run main.py
    else
        [[ -d .venv ]] || python3 -m venv .venv
        .venv/bin/python -m pip install --quiet -e .
        .venv/bin/python main.py
    fi

# Runs the Python TUI quickstart's credential-free CRUD self-test
python-tui-smoke:
    #!/usr/bin/env bash
    set -euo pipefail

    cd python-tui
    if command -v uv >/dev/null 2>&1; then
        uv run main.py --smoke
    else
        [[ -d .venv ]] || python3 -m venv .venv
        .venv/bin/python -m pip install --quiet -e .
        .venv/bin/python main.py --smoke
    fi
