#include "trie.hpp"
#include <cstdint>
#include <cstring>
#include <vector>

extern int max_recurse_depth;
extern int max_recurse_breadth;
extern int max_recurse_steps;
extern int speedup_numerator;

typedef enum CellState : uint8_t { UNKNOWN = 0, EMPTY = 1, ORANGE = 2, BLUE = 3, MINE = 4, SUPER_MINE = 5 } CellState;

typedef struct Position {
  uint8_t x, y;
} Position;

typedef struct Score {
  int orangeDistance, blueDistance;
  int orangeTiles, blueTiles;
} Score;

struct Move;

typedef struct DeltaScore {
  int gainedDistance, killedDistance;
  int gainedTiles, killedTiles;
  int wordLength;
  bool winning, losing;

  DeltaScore(int, bool, const Score &, const Score &);
} DeltaScore;

typedef struct Scoring {
  int gainedDistance, killedDistance;
  int gainedTiles, killedTiles;
  int wordLength;
  int winBonus, loseMinus;

  int score(const DeltaScore &) const;
  int score(int, bool, const Score &, const Score &) const;
} Scoring;

typedef struct Move {
  uint8_t *positions;
  uint8_t *word;
  uint len;
  int score;

  Move(const uint8_t *npos, const uint8_t *nword, uint len);
  Move(const Move &src);
  ~Move();

  Move &operator=(const Move &src) = delete;
} Move;

typedef struct GameStatic {
  uint8_t forwardMap[65536];
  char16_t backwardMap[256];
  trie<uint8_t> words;
  uint8_t board[130];
  bool playerOrange;
} GameCharMap;

typedef struct GameState {
  CellState states[130];
  bool orangeTurn;
} GameState;

extern GameStatic static_data;
extern GameState start_data;
extern Scoring scoring;

bool add_word(const char16_t *word, uint len);

bool remove_word(const char16_t *word, uint len);

bool init_mapping_and_board(const char16_t *board);

void apply_move(const Move *move, const GameState &input, GameState &output);

std::pair<Move, int> solve();
