package net.pietu1998.wordbasehacker;

import net.pietu1998.wordbasehacker.solver.Possibility;
import net.pietu1998.wordbasehacker.solver.Tile;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class BoardDrawable extends Drawable {

	private final Possibility pos;
	private final char[] tileLetters;
	private final boolean flipped;

	private final static float SQRT512 = (float) (16 * Math.sqrt(2));

	public BoardDrawable(Possibility pos, char[] tileLetters, boolean flipped) {
		this.pos = pos;
		this.tileLetters = tileLetters;
		this.flipped = flipped;
	}

	@Override
	public int getIntrinsicWidth() {
		return 800;
	}

	@Override
	public int getIntrinsicHeight() {
		return 1040;
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		if (pos.getResult() == null)
			return;

		Paint whiteBg = new Paint();
		whiteBg.setStyle(Paint.Style.FILL);
		whiteBg.setColor(0xFFFFFFFF);
		Paint orangeBg = new Paint(whiteBg);
		orangeBg.setColor(0xFFFE8800);
		Paint blueBg = new Paint(whiteBg);
		blueBg.setColor(0xFF00C9E8);
		Paint blackBg = new Paint(whiteBg);
		blackBg.setColor(0xFF000000);
		Paint purpleBg = new Paint(whiteBg);
		purpleBg.setColor(0xFF9239B2);

		Paint blackText = new Paint(whiteBg);
		blackText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		blackText.setTextSize(64);
		blackText.setColor(0xFF000000);
		Paint whiteText = new Paint(blackText);
		whiteText.setColor(0xFFFFFFFF);

		Paint path = new Paint();
		path.setStyle(Paint.Style.STROKE);
		path.setStrokeWidth(8);
		path.setStrokeCap(Paint.Cap.ROUND);
		path.setColor(0xFF009900);

		int[] tileStates = pos.getResult();
		for (int y = 0, index = 0; y < 13; y++) {
			for (int x = 0; x < 10; x++, index++) {
				int t = tileStates[index];
				if ((t & Tile.SUPER_MINE) != 0)
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, purpleBg);
				else if ((t & Tile.MINE) != 0)
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, blackBg);
				else if ((t & Tile.PLAYER) != 0)
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, flipped ? blueBg : orangeBg);
				else if ((t & Tile.OPPONENT) != 0)
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, flipped ? orangeBg : blueBg);
				else
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, whiteBg);
				canvas.drawText(String.valueOf(tileLetters[index]),
						x * 80 + 40 - blackText.measureText(String.valueOf(tileLetters[index])) / 2,
						y * 80 + 40 - blackText.ascent() / 2,
						(t & (Tile.MINE | Tile.SUPER_MINE)) != 0 ? whiteText : blackText);
			}
		}
		if (pos.getCoordinates().length > 2) {
			byte[] c = pos.getCoordinates();
			canvas.drawCircle(80 * c[0] + 40, 80 * c[1] + 40, 32, path);
			if (c[0] != c[2] && c[1] != c[3])
				canvas.drawLine(c[0] * 80 + (c[2] - c[0]) * SQRT512 + 40, c[1] * 80 + (c[3] - c[1])
						* SQRT512 + 40, c[2] * 80 + 40, c[3] * 80 + 40, path);
			else if (c[0] == c[2])
				canvas.drawLine(80 * c[0] + 40, 48 * c[1] + 32 * c[3] + 40, 80 * c[2] + 40, 80 * c[3] + 40,
						path);
			else
				canvas.drawLine(48 * c[0] + 32 * c[2] + 40, 80 * c[1] + 40, 80 * c[2] + 40, 80 * c[3] + 40,
						path);
			for (int i = 2; i < c.length - 2; i += 2) {
				canvas.drawLine(80 * c[i] + 40, 80 * c[i + 1] + 40, 80 * c[i + 2] + 40, 80 * c[i + 3] + 40, path);
			}
		}
	}

	@Override
	public void setAlpha(int alpha) {}

	@Override
	public void setColorFilter(ColorFilter cf) {}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

}
