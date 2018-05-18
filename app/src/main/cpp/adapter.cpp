#include "adapter.h"
#include "trie.hpp"
#include "solver.hpp"

#ifdef DEBUG
#ifdef __ANDROID__
#include <android/log.h>
#define LOG_D(msg) __android_log_print(ANDROID_LOG_DEBUG, "WordbaseHacker", msg);
#else
#include <iostream>
#define LOG_D(msg) std::cout << msg << std::endl;
#endif
#else
#define LOG_D(msg)
#endif

extern "C" {
JNIEXPORT void JNICALL Java_net_pietu1998_wordbasehacker_solver_NativeSolver_setScoring0(
        JNIEnv *, jclass,
        jint wordLength,
        jint gainedTiles, jint killedTiles,
        jint gainedDistance, jint killedDistance,
        jint winBonus, jint loseMinus) {
    LOG_D("Setting native solver scoring")
    scoring.wordLength = wordLength;
    scoring.gainedTiles = gainedTiles;
    scoring.killedTiles = killedTiles;
    scoring.gainedDistance = gainedDistance;
    scoring.killedDistance = killedDistance;
    scoring.winBonus = winBonus;
    scoring.loseMinus = loseMinus;
}

JNIEXPORT void JNICALL Java_net_pietu1998_wordbasehacker_solver_NativeSolver_setParams0(
        JNIEnv *, jclass,
        jint maxDepth, jint maxBreadth, jint maxSteps, jint speedupFactor) {
    LOG_D("Setting native solver params")
    max_recurse_depth = maxDepth;
    max_recurse_breadth = maxBreadth;
    max_recurse_steps = maxSteps;
    speedup_numerator = speedupFactor;
}

JNIEXPORT jboolean JNICALL Java_net_pietu1998_wordbasehacker_solver_NativeSolver_initBoard0(
        JNIEnv *env, jclass, jcharArray board) {
    LOG_D("Clearing word tree")
    static_data.words.clear();
    LOG_D("Initializing board and char mapping")
    uint16_t *chars = env->GetCharArrayElements(board, NULL);
    bool success = init_mapping_and_board((char16_t *) chars);
    env->ReleaseCharArrayElements(board, chars, JNI_ABORT);
    return (jboolean) success;
}

JNIEXPORT jboolean JNICALL Java_net_pietu1998_wordbasehacker_solver_NativeSolver_addWord0(
        JNIEnv *env, jclass, jstring word) {
    const uint16_t *chars = env->GetStringChars(word, NULL);
    const int32_t len = env->GetStringLength(word);
    bool success = add_word((char16_t *) chars, len);
    env->ReleaseStringChars(word, chars);
    return (jboolean) success;
}

JNIEXPORT jboolean JNICALL Java_net_pietu1998_wordbasehacker_solver_NativeSolver_removeWord0(
        JNIEnv *env, jclass, jstring word) {
    const uint16_t *chars = env->GetStringChars(word, NULL);
    const int32_t len = env->GetStringLength(word);
    bool success = remove_word((char16_t *) chars, len);
    env->ReleaseStringChars(word, chars);
    return (jboolean) success;
}

JNIEXPORT jint JNICALL Java_net_pietu1998_wordbasehacker_solver_NativeSolver_solve0(
        JNIEnv *env, jclass, jboolean playerOrange, jboolean orangeTurn, jbyteArray board,
        jbyteArray outPos, jcharArray outWord, jintArray outScore) {
    LOG_D("Starting solution")
    static_data.playerOrange = playerOrange;
    start_data.orangeTurn = orangeTurn;

    LOG_D("Copying cell states")
    int8_t *cells = env->GetByteArrayElements(board, NULL);
    memcpy(&start_data.states, cells, 130);
    env->ReleaseByteArrayElements(board, cells, JNI_ABORT);

    LOG_D("Finding solutions")
    std::pair<Move, int> result = solve();

    LOG_D("Returning solution to Java")
    int8_t *pos = env->GetByteArrayElements(outPos, NULL);
    uint16_t *chr = env->GetCharArrayElements(outWord, NULL);
    for (uint i = 0; i < result.first.len; i++) {
        pos[i * 2] = result.first.positions[i] % (uint8_t) 10;
        pos[i * 2 + 1] = result.first.positions[i] / (uint8_t) 10;
        chr[i] = static_data.backwardMap[result.first.word[i]];
    }
    env->ReleaseByteArrayElements(outPos, pos, 0);
    env->ReleaseCharArrayElements(outWord, chr, 0);

    LOG_D("Returning score to Java")
    int32_t *score = env->GetIntArrayElements(outScore, NULL);
    *score = result.second;
    env->ReleaseIntArrayElements(outScore, score, 0);

    return result.first.len;
}
}
