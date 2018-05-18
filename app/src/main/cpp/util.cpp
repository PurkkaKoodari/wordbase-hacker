#include "solver.hpp"
#include "util.hpp"
#include <iostream>

void print_utf16(const char16_t *str) {
  for (; *str; str++) {
    if (*str < 0x80)
      std::cout << (char)*str;
    else if (*str < 0x800)
      std::cout << (char)((*str >> 6) | 0xC0) << (char)((*str & 0x3F) | 0x80);
    else
      std::cout << (char)((*str >> 12) | 0xE0)
                << (char)(((*str >> 6) & 0x3F) | 0x80)
                << (char)((*str & 0x3F) | 0x80);
  }
}

void print_word(const Move *move) {
  char16_t unmapped[move->len + 1];
  for (uint k = 0; k < move->len; k++) {
    unmapped[k] = static_data.backwardMap[move->word[k]];
  }
  unmapped[move->len] = 0;
  print_utf16(unmapped);
}

void print_best(const Move *move, int score, int depth, bool opponent, bool best) {
  for (int i = 0; i < depth; i++)
    std::cout << "| ";
  if (move == NULL) {
    std::cout << "No moves?" << std::endl;
    return;
  }
  std::cout << (opponent ? "Opponent's" : best ? "Best" : "A") << " move " << (depth ? "will be " : "is ");
  print_word(move);
  std::cout << " from " << (move->positions[0] % 10) << ", " << (move->positions[0] / 10) << " (" << score << ")"
            << std::endl;
}

void print_result(const Move *move) {
  GameState afterState;
  apply_move(move, start_data, afterState);
  char16_t printBuf[2] = {0};
  for (uint y = 0, i = 0; y < 13; y++) {
    for (uint x = 0; x < 10; x++, i++) {
      bool used = false;
      for (uint j = 0; j < move->len; j++) {
        if (move->positions[j] == i) {
          used = true;
          break;
        }
      }
      std::cout << "\x1b[";
      switch (afterState.states[i]) {
      case EMPTY:
        std::cout << "47;30";
        break;
      case MINE:
        std::cout << "40;37";
        break;
      case SUPER_MINE:
        std::cout << "45;37";
        break;
      case ORANGE:
        std::cout << (used ? "41;30" : "43;30");
        break;
      case BLUE:
        std::cout << (used ? "44;30" : "46;30");
        break;
      default:
        break;
      }
      std::cout << "m";
      printBuf[0] = static_data.backwardMap[static_data.board[i]];
      print_utf16(printBuf);
    }
    std::cout << "\x1b[39;49m" << std::endl;
  }
}
