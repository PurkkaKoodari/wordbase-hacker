package net.pietu1998.wordbasehacker.solver;

public final class Tile {

	public static final int PLAYER = 1;
	public static final int OPPONENT = 2;
	public static final int MINE = 4;
	public static final int BASE = 8;
	public static final int SUPER_MINE = 16;

	public final int x, y, type;

	public Tile(int x, int y, int type) {
		this.x = x;
		this.y = y;
		this.type = type;
	}

}
