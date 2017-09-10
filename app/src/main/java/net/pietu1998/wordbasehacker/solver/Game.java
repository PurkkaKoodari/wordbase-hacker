package net.pietu1998.wordbasehacker.solver;

import java.util.HashSet;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;

public class Game implements Parcelable {

	private final int id, boardId, opponentId;
	private final String layout;
	private String opponent = null;
	private final Set<String> played;
	private final boolean flipped;

	public Game(int id, int boardId, int opponentId, String layout, boolean flipped) {
		this.id = id;
		this.boardId = boardId;
		this.opponentId = opponentId;
		this.layout = layout;
		this.played = new HashSet<>();
		this.flipped = flipped;
	}

	public String getOpponent() {
		return opponent;
	}

	public void setOpponent(String opponent) {
		this.opponent = opponent;
	}

	public int getId() {
		return id;
	}

	public int getBoardId() {
		return boardId;
	}

	public int getOpponentId() {
		return opponentId;
	}

	public String getLayout() {
		return layout;
	}

	public boolean isFlipped() {
		return flipped;
	}

	public void addWord(String word) {
		played.add(word);
	}

	public boolean isPlayed(String word) {
		return played.contains(word);
	}

	@Override
	public String toString() {
		return opponent == null ? "" : opponent;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeInt(boardId);
		dest.writeInt(opponentId);
		dest.writeString(layout);
		dest.writeByte((byte) (flipped ? 1 : 0));
		dest.writeString(opponent == null ? "" : opponent);
	}

	public static final Parcelable.Creator<Game> CREATOR = new Parcelable.Creator<Game>() {
		@Override
		public Game[] newArray(int size) {
			return new Game[size];
		}

		@Override
		public Game createFromParcel(Parcel source) {
			int id = source.readInt();
			int boardId = source.readInt();
			int opponentId = source.readInt();
			String layout = source.readString();
			boolean flipped = source.readByte() != 0;
			String opponent = source.readString();
			Game result = new Game(id, boardId, opponentId, layout, flipped);
			if (!opponent.equals(""))
				result.opponent = opponent;
			return result;
		}
	};

}
