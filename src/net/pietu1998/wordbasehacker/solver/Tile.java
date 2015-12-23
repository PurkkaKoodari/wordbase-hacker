package net.pietu1998.wordbasehacker.solver;

public class Tile {

	public static final int PLAYER = 1;
	public static final int OPPONENT = 2;
	public static final int MINE = 4;
	public static final int BASE = 8;

	private int flags;
	private char letter;

	public Tile(int flags, char letter) {
		this.flags = flags;
		this.letter = letter;
	}

	public int getFlags() {
		return flags;
	}

	public char getLetter() {
		return letter;
	}

	public boolean isSet(int flag) {
		return (flags & flag) != 0;
	}

	public Tile modify(int add, int remove) {
		return new Tile((flags | add) & ~remove, letter);
	}

}
