package net.pietu1998.wordbasehacker.solver;

public class Move {

	private final String layout, word;

	public Move(String layout, String word) {
		this.layout = layout;
		this.word = word;
	}

	public String getLayout() {
		return layout;
	}

	public String getWord() {
		return word;
	}
	
}
