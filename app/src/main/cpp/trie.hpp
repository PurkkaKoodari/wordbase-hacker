#ifndef TRIE_H
#define TRIE_H

#define TRIE_MAX_SIZE 256

#ifndef NULL
#define NULL 0
#endif

template <typename char_type>
struct trie {
    bool used;
    trie *children[TRIE_MAX_SIZE];

    trie() {
        this->used = false;
        for (int i = 0; i < TRIE_MAX_SIZE; i++) {
            this->children[i] = NULL;
        }
    }

    ~trie() {
        this->clear();
    }

    void clear() {
        for (int i = 0; i < TRIE_MAX_SIZE; i++) {
            if (this->children[i]) {
                delete this->children[i];
                this->children[i] = NULL;
            }
        }
        this->used = false;
    }
};

#endif
