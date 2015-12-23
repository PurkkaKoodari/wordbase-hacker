package net.pietu1998.wordbasehacker.solver;

import java.util.HashSet;
import java.util.Set;

public class Board {

	private Tile[][] tiles;
	private String[] words;

	public Board(Tile[][] tiles, String[] words) {
		this.tiles = tiles;
		this.words = words;
	}

	public Tile[][] getTiles() {
		return tiles;
	}

	public String[] getWords() {
		return words;
	}

	public void score(Possibility pos, boolean flipped) {
		Tile[][] newTiles = new Tile[10][13];
		int oldPlayer = 0, oldOpponent = 0, oldDistP = 0, oldDistO = 0;
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 13; y++) {
				newTiles[x][y] = tiles[x][y];
				if (newTiles[x][y].isSet(Tile.PLAYER)) {
					oldPlayer++;
					oldDistP = max(oldDistP, flipped ? 12 - y : y);
				} else if (newTiles[x][y].isSet(Tile.OPPONENT)) {
					oldOpponent++;
					oldDistO = max(oldDistO, flipped ? y : 12 - y);
				}
			}
		}

		for (int i = 0; i < pos.getCoordinates().length; i++)
			takeTile(newTiles, pos.getCoordinates()[i].x, pos.getCoordinates()[i].y);

		Set<Coordinate> connected = new HashSet<Coordinate>();
		addConnected(newTiles, 0, flipped ? 0 : 12, connected, flipped);
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 13; y++) {
				if (newTiles[x][y].isSet(Tile.OPPONENT) && !connected.contains(new Coordinate(x, y)))
					newTiles[x][y] = newTiles[x][y].modify(0, Tile.OPPONENT);
			}
		}

		int newPlayer = 0, newOpponent = 0, newDistP = 0, newDistO = 0;
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 13; y++) {
				if (newTiles[x][y].isSet(Tile.PLAYER)) {
					newPlayer++;
					newDistP = max(newDistP, flipped ? 12 - y : y);
				} else if (newTiles[x][y].isSet(Tile.OPPONENT)) {
					newOpponent++;
					newDistO = max(newDistO, flipped ? y : 12 - y);
				}
			}
		}
		pos.setScore(new Score(pos.getCoordinates().length, newPlayer - oldPlayer, oldOpponent - newOpponent, newDistP
				- oldDistP, oldDistO - newDistO, newDistP == 12));
		pos.setResult(new Board(newTiles, words));
	}

	private static int max(int a, int b) {
		return a > b ? a : b;
	}

	private void addConnected(Tile[][] tiles, int x, int y, Set<Coordinate> positions, boolean flipped) {
		if (x < 0 || x > 9 || y < 0 || y > 12 || positions.contains(new Coordinate(x, y))
				|| !tiles[x][y].isSet(Tile.OPPONENT))
			return;
		positions.add(new Coordinate(x, y));
		addConnected(tiles, x, y + 1, positions, flipped);
		addConnected(tiles, x - 1, y + 1, positions, flipped);
		addConnected(tiles, x + 1, y + 1, positions, flipped);
		addConnected(tiles, x - 1, y, positions, flipped);
		addConnected(tiles, x + 1, y, positions, flipped);
		addConnected(tiles, x, y - 1, positions, flipped);
		addConnected(tiles, x - 1, y - 1, positions, flipped);
		addConnected(tiles, x + 1, y - 1, positions, flipped);
	}

	private static void takeTile(Tile[][] tiles, int x, int y) {
		if (x < 0 || x > 9 || y < 0 || y > 12)
			return;
		if (tiles[x][y].isSet(Tile.MINE)) {
			tiles[x][y] = tiles[x][y].modify(0, Tile.MINE);
			takeTile(tiles, x, y - 1);
			takeTile(tiles, x - 1, y);
			takeTile(tiles, x + 1, y);
			takeTile(tiles, x, y + 1);
		}
		tiles[x][y] = tiles[x][y].modify(Tile.PLAYER, Tile.OPPONENT);
	}
}
