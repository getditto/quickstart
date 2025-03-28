#!/usr/bin/env awk -f

# This script is used to generate the "env.h" file that contains C++ definitions
# corresponding to the values in the ".env" file at the repository root.
#
# Run it like this, from the taskscpp directory:
#
#  awk -f scripts/generate_env.awk ../../.env > src/env.h

BEGIN {
  print "#ifndef DITTO_QUICKSTART_ENV_H"
  print "#define DITTO_QUICKSTART_ENV_H"
  print ""
  print "/***   GENERATED FILE - DO NOT EDIT   ***/"
  print ""
  print "// This file is generated by taskscpp/scripts/generate_env.awk, using .env as input."
  print "// Do not edit this file directly. Instead, edit .env and regenerate this file."
  print ""
}

# Skip blank lines.
 /^[[:space:]]*$/ { next }

# Skip lines that start with '#' (comments and shebangs).
 /^[[:space:]]*#/ { next }

# Process lines that look like key=value pairs.
{
    # Find the position of the first '=' in the line.
    pos = index($0, "=")
    if (pos > 0) {
        # Extract the key and value.
        key = substr($0, 1, pos-1)
        value = substr($0, pos+1)

        # Trim whitespace from key and value.
        gsub(/^[ \t]+/, "", key)
        gsub(/[ \t]+$/, "", key)
        gsub(/^[ \t]+/, "", value)
        gsub(/[ \t]+$/, "", value)

        # Verify that the key looks valid (starts with a capital letter or underscore,
        # followed by letters, digits, or underscores).
        if (key ~ /^[A-Z_][A-Z0-9_]*$/) {
            # If the value does not start and end with double quotes, add them.
            if (substr(value, 1, 1) != "\"" || substr(value, length(value), 1) != "\"") {
                value = "\"" value "\""
            }
            print "#define " key " " value
        }
    }
}

END {
    print ""
    print "#endif // DITTO_QUICKSTART_ENV_H"
}
