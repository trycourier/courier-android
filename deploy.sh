#!/bin/bash

cd scripts || { echo "Failed to change to scripts directory"; exit 1; }

error_exit() {
    echo "❌ Error: $1" >&2
    exit 1
}

declare -a steps=(
    "Run Tests:sh run_tests.sh"
    "Update SDK Version:sh update_version.sh"
    "Build Demo App:sh build_demo_app.sh"
    "Create Git Release:sh git_release.sh"
)

echo "Available steps:"
index=0
for step in "${steps[@]}"; do
    echo "$index: ${step%%:*}"
    ((index++))
done

read -p "Select step to start from (default 0): " start_step_index

if [[ ! $start_step_index =~ ^[0-9]+$ ]]; then
    start_step_index=0
fi

for (( i=start_step_index; i<${#steps[@]}; i++ )); do
    step="${steps[$i]}"
    title="${step%%:*}"
    script="${step#*:}"
    echo "Executing Step $i: $title"
    if ! eval "$script"; then
        error_exit "Step $i ($title) failed"
    fi
done

echo "All steps completed successfully."
