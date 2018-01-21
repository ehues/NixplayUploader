# Nixplay Uploader

Share photos to [Nix photo frames](https://www.nixplay.com). Differs from the [official app](https://play.google.com/store/apps/details?id=com.creedon.Nixplay) in that:
* the user can crop and rotate images before uploading them.
* there's a visual indicator when the image doesn't fit the 4:3 aspect ratio of the Nixplay Seed, allowing the user to avoid letterboxing when the image is shown on the frame.
* functionality is limited to sharing. This app does not provide other features (playlist management, updating profiles, adding friends, etc). 

Uses [uCrop](https://github.com/Yalantis/uCrop) for image rotation and cropping. 


## Disclaimer

This project is not affiliated with Nix. Their trademarks are used without permission. 


# Getting Started

It should be possible to fetch and build NixplayUploader directly from Android Studio. 


# Running Tests

There's only one test class, named [OnlineTest](app/src/test/java/dorian/nixplay/OnlineTest.java). It verifies that our Nix API works by doing a few queries and uploading a test image. You must provide it with two environment variables: `NIX_USERNAME` (the username of a working Nix account), and `NIX_PASSWORD` (the password for that account). Defining the variables in the Android Studio config prevents them from showing up on Github. 


# Contributing

Contributions are welcome. 


# Authors
 * Evan Hughes - Initial work
