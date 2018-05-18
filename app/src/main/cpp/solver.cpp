#include "solver.hpp"
#include "trie.hpp"

#ifdef PRINT_DECISIONS
#include "util.hpp"
#endif

#include <algorithm>
#include <climits>
#include <cstdint>
#include <vector>

int max_recurse_depth = 6;
int max_recurse_breadth = 10;
int max_recurse_steps = 15000;
int speedup_numerator = 245;

GameStatic static_data;
GameState start_data;
Score start_score;
Scoring scoring = {100, 400, 10, 60, 1, 1000000, -1000000};

Move::Move(const uint8_t *npos, const uint8_t *nword, uint len) {
    this->len = len;
    this->positions = new uint8_t[len];
    memcpy(this->positions, npos, len);
    this->word = new uint8_t[len];
    memcpy(this->word, nword, len);
}

Move::Move(const Move &src) {
    this->len = src.len;
    this->positions = new uint8_t[len];
    memcpy(this->positions, src.positions, len);
    this->word = new uint8_t[len];
    memcpy(this->word, src.word, len);
}

Move::~Move() {
    delete[] this->positions;
    delete[] this->word;
}

bool add_word(const char16_t *word, uint len) {
    uint j = 0;
    auto node = &static_data.words;
    for (; j < len; j++) {
        auto mapping = static_data.forwardMap[word[j]];
        if (!mapping)
            return false;
        --mapping;
        auto child = node->children[mapping];
        if (!child)
            node->children[mapping] = child = new trie<uint8_t>;
        node = child;
    }
    node->used = true;
    return true;
}

bool remove_word(const char16_t *word, uint len) {
    uint j = 0;
    auto node = &static_data.words;
    for (; j < len; j++) {
        auto mapping = static_data.forwardMap[word[j]];
        if (!mapping)
            return false;
        --mapping;
        auto child = node->children[mapping];
        if (!child)
            return true;
        node = child;
    }
    node->used = false;
    return true;
}

bool init_mapping_and_board(const char16_t *board) {
    for (uint i = 0; i < (1 << 16); i++)
        static_data.forwardMap[i] = 0;
    uint16_t charPos = 0;
    for (uint i = 0; i < 130; i++) {
        auto mapping = static_data.forwardMap[board[i]];
        if (!mapping) {
            if (charPos == TRIE_MAX_SIZE)
                return false;
            static_data.forwardMap[board[i]] = mapping = (uint8_t) ++charPos;
            static_data.backwardMap[mapping - 1] = board[i];
        }
        static_data.board[i] = mapping - (uint8_t) 1;
    }
    return true;
}

void find_moves_rec(trie<uint8_t> *words, uint8_t *word, uint8_t *positions, bool *visited, uint x,
                    uint y, uint i,
                    uint len, std::vector<Move *> &output) {
    if (visited[i])
        return;
    visited[i] = 1;
    uint8_t ch = static_data.board[i];
    word[len] = ch;
    positions[len] = (uint8_t) i;
    trie<uint8_t> *child = words->children[ch];
    if (child) {
        if (child->used)
            output.push_back(new Move(positions, word, len + 1));
        if (x > 0) {
            find_moves_rec(child, word, positions, visited, x - 1, y, i - 1, len + 1, output);
            if (y > 0)
                find_moves_rec(child, word, positions, visited, x - 1, y - 1, i - 11, len + 1,
                               output);
            if (y < 12)
                find_moves_rec(child, word, positions, visited, x - 1, y + 1, i + 9, len + 1,
                               output);
        }
        if (x < 9) {
            find_moves_rec(child, word, positions, visited, x + 1, y, i + 1, len + 1, output);
            if (y > 0)
                find_moves_rec(child, word, positions, visited, x + 1, y - 1, i - 9, len + 1,
                               output);
            if (y < 12)
                find_moves_rec(child, word, positions, visited, x + 1, y + 1, i + 11, len + 1,
                               output);
        }
        if (y > 0)
            find_moves_rec(child, word, positions, visited, x, y - 1, i - 10, len + 1, output);
        if (y < 12)
            find_moves_rec(child, word, positions, visited, x, y + 1, i + 10, len + 1, output);
    }
    visited[i] = 0;
}

void find_moves(std::vector<Move *> *output) {
    bool visited[130] = {0};
    uint8_t word[13];
    uint8_t positions[13];
    for (uint y = 0, i = 0; y < 13; y++) {
        for (uint x = 0; x < 10; x++, i++) {
            find_moves_rec(&static_data.words, word, positions, visited, x, y, i, 0, output[i]);
        }
    }
}

void count_cells(const GameState &state, Score &score) {
    score.orangeDistance = 0;
    score.blueDistance = 12;
    score.orangeTiles = 0;
    score.blueTiles = 0;
    bool countBlueDist = true;
    for (uint y = 0, i = 0; y < 13; y++) {
        for (uint x = 0; x < 10; x++, i++) {
            switch (state.states[i]) {
                case ORANGE:
                    score.orangeDistance = y;
                    score.orangeTiles++;
                    break;
                case BLUE:
                    if (countBlueDist) {
                        score.blueDistance = y;
                        countBlueDist = false;
                    }
                    score.blueTiles++;
                    break;
                default:
                    break;
            }
        }
    }
}

bool connectedCells[130];

void add_connected_rec(const GameState &state, uint x, uint i, CellState search) {
    if (connectedCells[i])
        return;
    if (state.states[i] != search)
        return;
    connectedCells[i] = true;
    if (i >= 10) {
        add_connected_rec(state, x, i - 10, search);
        if (x > 0)
            add_connected_rec(state, x - 1, i - 11, search);
        if (x < 9)
            add_connected_rec(state, x + 1, i - 9, search);
    }
    if (i < 120) {
        add_connected_rec(state, x, i + 10, search);
        if (x > 0)
            add_connected_rec(state, x - 1, i + 9, search);
        if (x < 9)
            add_connected_rec(state, x + 1, i + 11, search);
    }
    if (x > 0)
        add_connected_rec(state, x - 1, i - 1, search);
    if (x < 9)
        add_connected_rec(state, x + 1, i + 1, search);
}

void apply_move(const Move *move, const GameState &input, GameState &output) {
    CellState player;
    CellState opponent;
    uint opponentBase;
    if (input.orangeTurn) {
        player = ORANGE;
        opponent = BLUE;
        opponentBase = 12;
    } else {
        player = BLUE;
        opponent = ORANGE;
        opponentBase = 0;
    }
    for (uint i = 0; i < 130; i++)
        output.states[i] = input.states[i];
    for (uint i = 0; i < move->len; i++)
        output.states[move->positions[i]] = player;
    memset(connectedCells, false, 130);
    for (uint x = 0, start = 10 * opponentBase; x < 10; x++, start++)
        add_connected_rec(output, x, start, opponent);
    for (uint i = 0; i < 130; i++)
        if (!connectedCells[i] && output.states[i] == opponent)
            output.states[i] = EMPTY;
    output.orangeTurn = !input.orangeTurn;
}

bool ending_move(const Score &after, bool forOrange) {
    return forOrange ? (after.orangeDistance == 12) : (after.blueDistance == 0);
}

DeltaScore::DeltaScore(int wordLength, bool forOrange, const Score &before, const Score &after) {
    this->wordLength = wordLength;
    if (forOrange) {
        this->gainedTiles = after.orangeTiles - before.orangeTiles;
        this->killedTiles = before.blueTiles - after.blueTiles;
        this->gainedDistance = after.orangeDistance - before.orangeDistance;
        this->killedDistance = after.blueDistance - before.blueDistance;
        this->winning = after.orangeDistance == 12;
        this->losing = after.orangeDistance == 0;
    } else {
        this->gainedTiles = after.blueTiles - before.blueTiles;
        this->killedTiles = before.orangeTiles - after.orangeTiles;
        this->gainedDistance = before.blueDistance - after.blueDistance;
        this->killedDistance = before.orangeDistance - after.orangeDistance;
        this->winning = after.blueDistance == 0;
        this->losing = after.orangeDistance == 12;
    }
}

int Scoring::score(int wordLength, bool forOrange, const Score &before, const Score &after) const {
    int score = this->wordLength * wordLength;
    if (forOrange) {
        score += this->gainedTiles * (after.orangeTiles - before.orangeTiles);
        score += this->killedTiles * (before.blueTiles - after.blueTiles);
        score += this->gainedDistance * (after.orangeDistance - before.orangeDistance);
        score += this->killedDistance * (after.blueDistance - before.blueDistance);
        score += this->winBonus * (after.orangeDistance == 12);
        score += this->loseMinus * (after.blueDistance == 0);
    } else {
        score += this->gainedTiles * (after.blueTiles - before.blueTiles);
        score += this->killedTiles * (before.orangeTiles - after.orangeTiles);
        score += this->gainedDistance * (before.blueDistance - after.blueDistance);
        score += this->killedDistance * (before.orangeDistance - after.orangeDistance);
        score += this->winBonus * (after.blueDistance == 0);
        score += this->loseMinus * (after.orangeDistance == 12);
    }
    return score;
}

int Scoring::score(const DeltaScore &delta) const {
    int score = this->wordLength * delta.wordLength;
    score += this->gainedTiles * delta.gainedTiles;
    score += this->killedTiles * delta.killedTiles;
    score += this->gainedDistance * delta.gainedDistance;
    score += this->killedDistance * delta.killedDistance;
    score += this->winBonus * delta.winning;
    score += this->loseMinus * delta.losing;
    return score;
}

bool word_in_trie(const uint8_t *word, uint len, const trie<uint8_t> *node) {
    if (len) {
        const trie<uint8_t> *child = node->children[*word];
        return child && word_in_trie(word + 1, len - 1, child);
    } else {
        return node->used;
    }
}

void add_to_trie(const uint8_t *word, uint len, trie<uint8_t> *node) {
    if (len) {
        add_to_trie(word + 1, len - 1, node->children[*word]);
    } else {
        node->used = true;
    }
}

void remove_from_trie(const uint8_t *word, uint len, trie<uint8_t> *node) {
    if (len) {
        remove_from_trie(word + 1, len - 1, node->children[*word]);
    } else {
        node->used = false;
    }
}

struct WordComparer {
    inline bool operator()(const Move *lhs, const Move *rhs) { return lhs->score > rhs->score; }
};

std::pair<Move *, int> solve_rec_opponent(std::vector<Move *> *,
                                          const GameState &, const Score &, uint, long &);

std::pair<Move *, int> solve_rec_player(std::vector<Move *> *,
                                        const GameState &, const Score &, uint, long &);

std::pair<Move *, int> solve_rec_opponent(std::vector<Move *> *moves,
                                          const GameState &beforeState, const Score &before,
                                          uint depth, long &totalEnds) {
    Score after;
    GameState afterState;
    CellState player = beforeState.orangeTurn ? ORANGE : BLUE;
    Move *best = NULL;
    int bestScore = INT32_MIN;
    for (uint i = 0; i < 130; i++) {
        if (beforeState.states[i] != player)
            continue;
        auto &movesHere = moves[i];
        for (auto iter = movesHere.begin(); iter != movesHere.end(); iter++) {
            Move *move = *iter;
            if (!word_in_trie(move->word, move->len, &static_data.words))
                continue;
            apply_move(move, beforeState, afterState);
            count_cells(afterState, after);
            if (ending_move(after, beforeState.orangeTurn)) {
                best = move;
                goto bestKnown;
            }
            int score = scoring.score(move->len, beforeState.orangeTurn, before, after);
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
    }
    bestKnown:;
    apply_move(best, beforeState, afterState);
    count_cells(afterState, after);
    if (!ending_move(after, beforeState.orangeTurn)) {
        remove_from_trie(best->word, best->len, &static_data.words);
        auto result = solve_rec_player(moves, afterState, after, depth + 1, totalEnds);
        add_to_trie(best->word, best->len, &static_data.words);
#if PRINT_DECISIONS
        print_best(best, result.second, depth, true, true);
#endif
        return std::make_pair(best, result.second);
    } else {
        int finalScore = scoring.score(best->len, static_data.playerOrange, start_score, after);
#if PRINT_DECISIONS
        print_best(best, finalScore, depth, true, true);
#endif
        return std::make_pair(best, finalScore);
    }
}

std::pair<Move *, int> solve_rec_player(std::vector<Move *> *moves,
                                        const GameState &beforeState, const Score &before,
                                        uint depth, long &totalEnds) {
    Score after;
    GameState afterState;
    std::vector<Move *> topMoves;
    CellState player = beforeState.orangeTurn ? ORANGE : BLUE;
    Move *best = NULL;
    int bestScore = INT32_MIN;
    bool canWinDirectly = false;
    for (uint i = 0; i < 130; i++) {
        if (beforeState.states[i] != player)
            continue;
        auto &movesHere = moves[i];
        for (auto iter = movesHere.begin(); iter != movesHere.end(); iter++) {
            Move *move = *iter;
            if (!word_in_trie(move->word, move->len, &static_data.words))
                continue;
            apply_move(move, beforeState, afterState);
            count_cells(afterState, after);
            if (ending_move(after, beforeState.orangeTurn)) {
                if (depth != 0) {
                    best = move;
                    goto winDirectly;
                }
                canWinDirectly = true;
            }
            int score = scoring.score(move->len, beforeState.orangeTurn, before, after);
            if (score > bestScore) {
                bestScore = score;
                best = move;
            } else if (score < bestScore / 2) {
                continue;
            }
            move->score = score;
            topMoves.push_back(move);
        }
    }
    if (!canWinDirectly) {
        best = NULL;
        bestScore = INT32_MIN;
        std::sort(topMoves.begin(), topMoves.end(), WordComparer());
        auto iter = topMoves.begin();
        for (int i = 0; i < max_recurse_breadth && totalEnds < max_recurse_steps &&
                        iter != topMoves.end(); i++, iter++) {
            totalEnds++;
            Move *attempt = *iter;
            apply_move(attempt, beforeState, afterState);
            count_cells(afterState, after);
            int score;
            if (depth < max_recurse_depth && !ending_move(after, beforeState.orangeTurn)) {
                remove_from_trie(attempt->word, attempt->len, &static_data.words);
                auto result = solve_rec_opponent(moves, afterState, after, depth + 1, totalEnds);
                add_to_trie(attempt->word, attempt->len, &static_data.words);
                score = (result.second * speedup_numerator) >> 8;
            } else {
                score = scoring.score(attempt->len, static_data.playerOrange, start_score, after);
            }
            if (score > bestScore) {
                best = attempt;
                bestScore = score;
#if PRINT_DECISIONS
                print_best(attempt, score, depth, false, true);
#endif
            } else {
#if PRINT_DECISIONS
                print_best(attempt, score, depth, false, false);
#endif
            }
        }
        return std::make_pair(best, bestScore);
    }
    winDirectly:;
    int finalScore = scoring.score(best->len, static_data.playerOrange, start_score, after);
    return std::make_pair(best, finalScore);
}

std::pair<Move, int> solve() {
    count_cells(start_data, start_score);
    long totalEnds = 0;
    std::vector<Move *> moves[130];
    find_moves(moves);
    auto result = start_data.orangeTurn == static_data.playerOrange ?
                  solve_rec_player(moves, start_data, start_score, 0, totalEnds) :
                  solve_rec_opponent(moves, start_data, start_score, 0, totalEnds);
    Move best = *result.first;
    for (uint i = 0; i < 130; i++) {
        auto &movesHere = moves[i];
        for (auto iter = movesHere.begin(); iter != movesHere.end(); iter++)
            delete *iter;
    }
    return std::make_pair(best, result.second);
}
