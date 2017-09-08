package net.pietu1998.wordbasehacker.solver;

public class Coordinate {

	public final int x, y;
	private final int index;

	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
		this.index = y + 10 * x;
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Coordinate))
			return false;
		Coordinate other = (Coordinate) obj;
		return x == other.x && y == other.y;
	}

}
