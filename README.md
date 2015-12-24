# Wordbase Hacker

Wordbase Hacker is a simple application that takes advantage of the fact that
the popular Android game Wordbase has its data stored in an unencrypted SQLite
file in its /data/ folder.

The data file contains all the games for the user, including _all the words
found on the boards_. This makes it rather simple to write a program to find
the optimal word in a situation based on some calculation.

Wordbase Hacker requires root access to get the app's data from its secure
folder.

Wordbase Hacker is licensed under the [MIT license](src/master/LICENSE).

### Implemented features

  - autoloading data from app folder with root
  - finding possible words
  - scoring words on different categories
  - displaying result exactly as in the game

### To be implemented

  - HUD to be placed over game view
  - automatic playing (?)