# Pocketpen logo animation

Remotion project that renders the intro shown on first launch.

```
npm install
npx remotion render src/index.ts Intro out/intro.mp4 --codec=h264 --pixel-format=yuv420p --crf=26 --gl=swangle
ffmpeg -y -i out/intro.mp4 -an -c:v libx264 -profile:v baseline -level 3.1 -pix_fmt yuv420p -crf 24 -movflags +faststart ../../app/src/main/res/raw/pp_intro.mp4
```

About page logo (square, looping):

```
npx remotion render src/index.ts About out/about.mp4 --codec=h264 --pixel-format=yuv420p --crf=26 --gl=swangle
ffmpeg -y -i out/about.mp4 -an -c:v libx264 -profile:v baseline -level 3.1 -pix_fmt yuv420p -crf 26 -movflags +faststart ../../app/src/main/res/raw/pp_about.mp4
```

Add `--browser-executable=<path to chrome-headless-shell>` if Remotion cannot download its own browser.
