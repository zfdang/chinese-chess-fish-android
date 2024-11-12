#!/bin/bash
echo "Build APK"
./gradlew assembleRelease

echo "copy apk"
rm docs/apk/*.apk
cp app/build/outputs/apk/release/*.apk docs/apk/

## find apk filename
cd ./docs/apk/
APK="$(ls *.apk)"
git lfs track "*.apk"
echo $APK

## grep file
cd ../
OLDAPK="$(grep -o -E -i 'ChessFish_\d{8}_\d{3}_release\.apk' release.html | head -1)"
echo $OLDAPK

echo "update release.html"
sed -i -e "s/$OLDAPK/$APK/g" release.html

echo "push"
git add .
git commit -a -m "update apk"
git push