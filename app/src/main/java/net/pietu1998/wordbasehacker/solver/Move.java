package net.pietu1998.wordbasehacker.solver;

import java.util.List;

public class Move {

    private final List<Tile> layout;
    private final String word;

    public Move(List<Tile> layout, String word) {
        this.layout = layout;
        this.word = word;
    }

    public List<Tile> getLayout() {
        return layout;
    }

    public String getWord() {
        return word;
    }

}
