#!/bin/bash

mkdir assets
ASSETS=$(realpath assets)
RECIPES_PATH=$(realpath testmod/run/recipes)

rm -rf "$RECIPES_PATH"

./gradlew :testmod:runClientGametest

pushd $RECIPES_PATH || exit

for folder in *; do
    FILES=$(printf '%s\n' "$folder"/* | sort | perl -pe 'chomp if eof' | tr '\n' ' ')
    if grep -qv ' ' <<< "$FILES"; then
        cp "$FILES" "$ASSETS/$folder.png"
    elif grep -qP 'smelting|smoking|blasting' <<< "$FILES"; then
        gifski -r 10 \
            --extra \
            -Q 100 \
            --motion-quality 100 \
            --lossy-quality 100 \
            --repeat 0 \
            --output "$ASSETS/$folder.gif" \
            "$FILES"
    else
        gifski -r 1 \
            --extra \
            -Q 100 \
            --motion-quality 100 \
            --lossy-quality 100 \
            --repeat 0 \
            --output "$ASSETS/$folder.gif" \
            "$FILES"
    fi
done

popd || exit
