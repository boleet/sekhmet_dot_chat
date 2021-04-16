#!/bin/bash

cd frontend
npm run build
cd ..

# Vue has been compiled, now move these files into the backend so that they can be served

rm ./backend/src/main/resources/static/css/*
rm ./backend/src/main/resources/static/js/*

mv ./frontend/build/index.html ./backend/src/main/resources/templates
mv ./frontend/build/favicon.png ./backend/src/main/resources/static
mv ./frontend/build/js/* ./backend/src/main/resources/static/js
mv ./frontend/build/css/* ./backend/src/main/resources/static/css

echo "Done :D"
echo "Files have been moved into the backend."
