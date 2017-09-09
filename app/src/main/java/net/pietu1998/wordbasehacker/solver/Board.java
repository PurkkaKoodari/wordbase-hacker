package net.pietu1998.wordbasehacker.solver;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Board {

	private short[] charMap = new short[65536];
	private short charIndex = 0;

	private int[] tileStates;
	private char[] tileLetters;
	private short[] tileMappedLetters;

	private TrieNode root;

	private List<Possibility> results = new ArrayList<>();

	public List<Possibility> getResults() {
		return results;
	}

	private class TrieNode {
		private TrieNode[] nodes;
		private boolean hasChildren = false;
		private String content = null;

		TrieNode() {
			nodes = new TrieNode[charIndex];
		}
	}

	public Board(String[] rows, int[] tileStates, String[] words, Game game) {
		this.tileStates = tileStates;
		tileLetters = new char[130];
		tileMappedLetters = new short[130];
		for (int y = 0, index = 0; y < 13; y++) {
			for (int x = 0; x < 10; x++, index++) {
				char letter = tileLetters[index] = rows[y].charAt(x);
				int mapping = charMap[letter];
				if (mapping == 0)
					mapping = charMap[letter] = ++charIndex;
				tileMappedLetters[index] = (short) (mapping - 1);
			}
		}
		root = new TrieNode();
		for (String word : words) {
			if (game.isPlayed(word))
				continue;
			Board.TrieNode node = root;
			for (int i = 0; i < word.length(); i++) {
				int mapping = charMap[word.charAt(i)] - 1;
				node.hasChildren = true;
				Board.TrieNode next = node.nodes[mapping];
				if (next == null)
					node = node.nodes[mapping] = new Board.TrieNode();
				else
					node = next;
			}
			node.content = word;
		}
	}

	public void findWords() {
		long start = System.currentTimeMillis();
		for (int iteration = 0; iteration < 500; iteration++) {
			boolean positions[] = new boolean[130];
			byte coords[] = new byte[24];
			for (int y = 0, index = 0; y < 13; y++)
				for (int x = 0; x < 10; x++, index++)
					if ((tileStates[index] & Tile.PLAYER) != 0)
						findWordsRecursive(tileMappedLetters, 0, x, y, index, coords, positions, root, results);
			if (iteration < 499) results.clear();
		}
		long end = System.currentTimeMillis();
		Log.d("WordbaseHacker", "Finding took " + (end - start) + "ms");
	}

	private void findWordsRecursive(short[] tiles, int length, int x, int y, int index, byte[] coords, boolean[] positions, TrieNode node, List<Possibility> results) {
		if (length >= 12 || x < 0 || x > 9 || y < 0 || y > 12 || positions[index])
			return;
		short letter = tiles[index];
		node = node.nodes[letter];
		if (node == null)
			return;
		coords[length * 2] = (byte) x;
		coords[length * 2 + 1] = (byte) y;
		if (node.content != null)
			results.add(new Possibility(Arrays.copyOf(coords, length * 2 + 2), node.content));
		if (!node.hasChildren)
			return;
		positions[index] = true;
		findWordsRecursive(tiles, length + 1, x, y + 1, index + 10, coords, positions, node, results);
		findWordsRecursive(tiles, length + 1, x - 1, y + 1, index + 9, coords, positions, node, results);
		findWordsRecursive(tiles, length + 1, x + 1, y + 1, index + 11, coords, positions, node, results);
		findWordsRecursive(tiles, length + 1, x - 1, y, index - 1, coords, positions, node, results);
		findWordsRecursive(tiles, length + 1, x + 1, y, index + 1, coords, positions, node, results);
		findWordsRecursive(tiles, length + 1, x, y - 1, index - 10, coords, positions, node, results);
		findWordsRecursive(tiles, length + 1, x - 1, y - 1, index - 11, coords, positions, node, results);
		findWordsRecursive(tiles, length + 1, x + 1, y - 1, index - 9, coords, positions, node, results);
		positions[index] = false;
	}

	public void scoreWords(boolean flipped) {
		long start = System.currentTimeMillis();
		for (int iteration = 0; iteration < 100; iteration++)
			for (Possibility pos : results) {
				scoreWord(pos, flipped);
				pos.setTileLetters(tileLetters);
			}
		long end = System.currentTimeMillis();
		Log.d("WordbaseHacker", "Scoring took " + (end - start) + "ms");
	}

	private void scoreWord(Possibility pos, boolean flipped) {
		int[] newStates = new int[130];
		int oldMines = 0, oldPlayer = 0, oldOpponent = 0, oldDistPlayer = 0, oldDistOpponent = 0;
		for (int y = 0, index = 0; y < 13; y++) {
			for (int x = 0; x < 10; x++, index++) {
				int state = newStates[index] = tileStates[index];
				if ((state & Tile.PLAYER) != 0) {
					oldPlayer++;
					oldDistPlayer = Math.max(oldDistPlayer, flipped ? 12 - y : y);
				} else if ((state & Tile.OPPONENT) != 0) {
					oldOpponent++;
					oldDistOpponent = Math.max(oldDistOpponent, flipped ? y : 12 - y);
				} else if ((state & (Tile.MINE | Tile.SUPER_MINE)) != 0) {
					oldMines++;
				}
			}
		}

		byte[] coords = pos.getCoordinates();
		for (int i = 0; i < coords.length; i += 2) {
			byte x = coords[i];
			byte y = coords[i + 1];
			takeTile(newStates, x, y, x + 10 * y);
		}

		boolean[] connected = new boolean[130];
		int baseY = flipped ? 0 : 12;
		int offset = 10 * baseY;
		for (int x = 0; x < 10; x++)
			addConnected(newStates, x, baseY, x + offset, connected);

		for (int index = 0; index < 130; index++)
			if (!connected[index])
				newStates[index] &= ~Tile.OPPONENT;

		int newMines = 0, newPlayer = 0, newOpponent = 0, newDistPlayer = 0, newDistOpponent = 0;
		for (int y = 0, index = 0; y < 13; y++) {
			for (int x = 0; x < 10; x++, index++) {
				int state = newStates[index];
				if ((state & Tile.PLAYER) != 0) {
					newPlayer++;
					newDistPlayer = Math.max(newDistPlayer, flipped ? 12 - y : y);
				} else if ((state & Tile.OPPONENT) != 0) {
					newOpponent++;
					newDistOpponent = Math.max(newDistOpponent, flipped ? y : 12 - y);
				} else if ((state & (Tile.MINE | Tile.SUPER_MINE)) != 0) {
					newMines++;
				}
			}
		}
		pos.setScore(new Score(pos.getCoordinates().length / 2, oldMines - newMines,
				newPlayer - oldPlayer, oldOpponent - newOpponent,
				newDistPlayer - oldDistPlayer, oldDistOpponent - newDistOpponent,
				newDistPlayer == 12));
		pos.setResult(newStates);
	}

	private void takeTile(int[] tiles, int x, int y, int index) {
		if (x < 0 || x > 9 || y < 0 || y > 12)
			return;
		int state = tiles[index];
		if (state != (state &= ~Tile.SUPER_MINE)) {
			takeTile(tiles, x - 1, y - 1, index - 11);
			takeTile(tiles, x + 1, y - 1, index - 9);
			takeTile(tiles, x - 1, y + 1, index + 9);
			takeTile(tiles, x + 1, y + 1, index + 11);
			takeTile(tiles, x, y - 1, index - 10);
			takeTile(tiles, x - 1, y, index - 1);
			takeTile(tiles, x + 1, y, index + 1);
			takeTile(tiles, x, y + 1, index + 10);
		} else if (state != (state &= ~Tile.MINE)) {
			takeTile(tiles, x, y - 1, index - 10);
			takeTile(tiles, x - 1, y, index - 1);
			takeTile(tiles, x + 1, y, index + 1);
			takeTile(tiles, x, y + 1, index + 10);
		}
		tiles[index] = (state & ~Tile.OPPONENT) | Tile.PLAYER;
	}

	private void addConnected(int[] tiles, int x, int y, int index, boolean[] positions) {
		if (x < 0 || x > 9 || y < 0 || y > 12 || positions[index] || (tiles[index] & Tile.OPPONENT) == 0)
			return;
		positions[index] = true;
		addConnected(tiles, x - 1, y - 1, index - 11, positions);
		addConnected(tiles, x, y - 1, index - 10, positions);
		addConnected(tiles, x + 1, y - 1, index - 9, positions);
		addConnected(tiles, x - 1, y, index - 1, positions);
		addConnected(tiles, x + 1, y, index + 1, positions);
		addConnected(tiles, x - 1, y + 1, index + 9, positions);
		addConnected(tiles, x, y + 1, index + 10, positions);
		addConnected(tiles, x + 1, y + 1, index + 11, positions);
	}
}
