#include "solver.hpp"
#include "trie.hpp"
#include "util.hpp"
#include <cstring>
#include <iostream>
#include <sqlite3.h>
#include <stdexcept>
#include <vector>

bool player_list(sqlite3 *db);

bool try_init_game(sqlite3 *db, int id);

void print_utf16(const char16_t *str);

int main(int argc, const char **argv) {
  int gameId = 0;
  if (argc == 3) {
    try {
      gameId = std::stoi(argv[2]);
    } catch (std::invalid_argument) {
    } catch (std::out_of_range) {
    }
  }
  if (argc < 2 || argc > 3 || (argc == 3 && gameId == 0)) {
    std::cerr << "Usage: " << argv[0] << " <wordbase.db> [gameId]" << std::endl;
    return 1;
  }
  sqlite3 *db;
  auto status = sqlite3_open_v2(argv[1], &db, SQLITE_OPEN_READONLY, NULL);
  if (status != SQLITE_OK) {
    std::cerr << "Failed to open database: " << status << std::endl;
    return 1;
  }
  sqlite3_extended_result_codes(db, 1);
  if (gameId != 0) {
    if (try_init_game(db, gameId)) {
      auto result = solve();
      print_best(&result.first, result.second, 0, false, true);
      print_result(&result.first);
    } else {
      std::cerr << "Failed to init game" << std::endl;
    }
  } else {
    if (!player_list(db)) {
      sqlite3_close_v2(db);
      return 1;
    }
  }
  sqlite3_close_v2(db);
  return 0;
}

bool player_list(sqlite3 *db) {
  sqlite3_stmt *stmt;
  auto status = sqlite3_prepare_v2(db,
                                   "SELECT games._id, players.first_name, turn = 'player' AS playerTurn "
                                   "FROM games, players "
                                   "WHERE winner IS NULL AND games.opponent_id = players._id "
                                   "ORDER BY playerTurn DESC",
                                   -1, &stmt, NULL);
  if (status != SQLITE_OK) {
    std::cerr << "Failed to get game list: " << sqlite3_errmsg(db) << std::endl;
    return false;
  }
  int prevTurn = -1;
  while (sqlite3_step(stmt) == SQLITE_ROW) {
    auto gameId = sqlite3_column_int(stmt, 0);
    auto opponentName = sqlite3_column_text(stmt, 1);
    auto playerTurn = sqlite3_column_int(stmt, 2);
    if (playerTurn != prevTurn) {
      prevTurn = playerTurn;
      std::cout << (playerTurn ? "Your turn:" : "Opponent's turn:") << std::endl;
    }
    std::cout << "[" << gameId << "] " << opponentName << std::endl;
  }
  sqlite3_finalize(stmt);
  return true;
}

bool apply_move(const unsigned char *move, int len);

bool try_init_game(sqlite3 *db, int id) {
  sqlite3_stmt *stmt;
  auto status = sqlite3_prepare_v2(db,
                                   "SELECT boards.rows, boards.words, "
                                   "games.turn = 'player' AS playerTurn, "
                                   "games.owner = 'player' AS playerOrange, "
                                   "games.layout "
                                   "FROM games, boards "
                                   "WHERE games._id = :id AND boards._id = games.board_id",
                                   -1, &stmt, NULL);
  if (status != SQLITE_OK) {
    std::cerr << "Failed to get game: " << sqlite3_errmsg(db) << std::endl;
    return false;
  }
  sqlite3_bind_int(stmt, 1, id);
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    bool playerActuallyOrange = sqlite3_column_int(stmt, 3);
    bool actuallyPlayerTurn = sqlite3_column_int(stmt, 2);
    static_data.playerOrange = playerActuallyOrange;
    start_data.orangeTurn = playerActuallyOrange == actuallyPlayerTurn;
    const char16_t *boardRows = (const char16_t *)sqlite3_column_text16(stmt, 0);
    int rowsLen = sqlite3_column_bytes16(stmt, 0) >> 1;
    char16_t board[130];
    for (int i = 0, boardPos = 0; i < rowsLen; i++) {
      char16_t ch = boardRows[i];
      if (!ch || ch == '[' || ch == ',' || ch == ' ' || ch == ']')
        continue;
      if (boardPos >= 130) {
        std::cerr << "Too long board in game" << std::endl;
        sqlite3_finalize(stmt);
        return false;
      }
      board[boardPos++] = ch;
    }
    if (!init_mapping_and_board(board)) {
      sqlite3_finalize(stmt);
      std::cerr << "Character mapping failed" << std::endl;
      return false;
    }
    const char16_t *boardWords = (const char16_t *)sqlite3_column_text16(stmt, 1);
    int wordsLen = sqlite3_column_bytes16(stmt, 1) >> 1;
    char16_t word[13];
    std::vector<const char16_t *> words;
    for (int i = 0, wordLen = 0; i < wordsLen; i++) {
      char16_t ch = boardWords[i];
      if ((!ch || ch == ',' || ch == ']') && wordLen > 0) {
        word[wordLen] = 0;
        if (!add_word(word, wordLen)) {
          std::cerr << "Failed to add word to game" << std::endl;
          sqlite3_finalize(stmt);
          return false;
        }
        wordLen = 0;
      }
      if (!ch || ch == '[' || ch == ',' || ch == ' ' || ch == ']')
        continue;
      if (wordLen >= 12) {
        std::cerr << "Too long word found in game" << std::endl;
        sqlite3_finalize(stmt);
        return false;
      }
      word[wordLen++] = ch;
    }
    const unsigned char *gameLayout = sqlite3_column_text(stmt, 4);
    int layoutLen = sqlite3_column_bytes(stmt, 4);
    for (int i = 0; i < 130; i++)
      start_data.states[i] = UNKNOWN;
    if (!apply_move(gameLayout, layoutLen)) {
      sqlite3_finalize(stmt);
      return false;
    }
    sqlite3_finalize(stmt);
    auto status = sqlite3_prepare_v2(db,
                                     "SELECT fields, word "
                                     "FROM moves "
                                     "WHERE game_id = :id",
                                     -1, &stmt, NULL);
    if (status != SQLITE_OK) {
      std::cerr << "Failed to get game: " << sqlite3_errmsg(db) << std::endl;
      return false;
    }
    sqlite3_bind_int(stmt, 1, id);
    while (sqlite3_step(stmt) == SQLITE_ROW) {
      const unsigned char *moveLayout = sqlite3_column_text(stmt, 0);
      layoutLen = sqlite3_column_bytes(stmt, 0);
      if (!apply_move(moveLayout, layoutLen)) {
        sqlite3_finalize(stmt);
        return false;
      }
      const char16_t *playedWord = (const char16_t *)sqlite3_column_text16(stmt, 1);
      int wordLen = sqlite3_column_bytes16(stmt, 1) >> 1;
      if (!remove_word(playedWord, wordLen)) {
        std::cerr << "Invalid move in game" << std::endl;
        sqlite3_finalize(stmt);
        return false;
      }
    }
    sqlite3_finalize(stmt);
    // play as opponent if not in turn
    static_data.playerOrange = playerActuallyOrange == actuallyPlayerTurn;
    start_data.orangeTurn = static_data.playerOrange;
    return true;
  }
  sqlite3_finalize(stmt);
  return false;
}

bool apply_move(const unsigned char *move, int len) {
  try {
    enum ParseMode : uint8_t { START, TYPE, XPOS, YPOS, OWNER } mode = START;
    int x, y, partStart = 0;
    CellState type;
    unsigned char posBuf[4] = {0};
    for (int i = 1; i < len - 1; i++) {
      char16_t ch = move[i];
      if (ch == '[') {
        if (mode != START) {
          std::cerr << "Unexpected [ in layout in game" << std::endl;
          return false;
        }
        mode = TYPE;
        partStart = i + 1;
      } else if (ch == ']') {
        if (mode != OWNER) {
          std::cerr << "Unexpected ] in layout in game" << std::endl;
          return false;
        }
        if (move[partStart + 1] == 'p')
          start_data.states[x + 10 * y] = static_data.playerOrange ? ORANGE : BLUE;
        else if (move[partStart + 1] == 'o')
          start_data.states[x + 10 * y] = static_data.playerOrange ? BLUE : ORANGE;
        else
          start_data.states[x + 10 * y] = type;
        mode = START;
      } else if (ch == ',') {
        if (mode == START)
          continue;
        if (mode == XPOS || mode == YPOS) {
          if (i - partStart > 4) {
            std::cerr << "Too long number in layout in game" << std::endl;
            return false;
          }
          std::fill(posBuf, posBuf + 4, 0);
          memcpy(posBuf, move + partStart + 1, i - partStart - 2);
          x = y;
          y = std::stoi((char *)posBuf);
        } else if (mode == TYPE) {
          if (move[partStart + 1] == 'M') {
            type = MINE;
          } else if (move[partStart + 1] == 'S') {
            type = SUPER_MINE;
          } else {
            type = EMPTY;
          }
        } else {
          std::cerr << "Unexpected , in layout in game" << std::endl;
          return false;
        }
        mode = (ParseMode)((uint8_t)mode + 1);
        partStart = i + 1;
      }
    }
    return true;
  } catch (std::invalid_argument) {
    std::cerr << "Invalid number in layout in game" << std::endl;
    return false;
  }
}
