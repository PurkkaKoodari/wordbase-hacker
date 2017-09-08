package net.pietu1998.wordbasehacker.solver;

public class Coordinate {

	public final int x, y;
	private final int hash;

	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
		this.hash = (x << 8) | y;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Coordinate))
			return false;
		Coordinate other = (Coordinate) obj;
		return x == other.x && y == other.y;
	}

}
