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
		drawPath(canvas, pos.getCoordinates());
	}

	public static void drawPath(@NonNull Canvas canvas, byte[] coords) {
		if (coords.length <= 2)
			return;
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(8);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setColor(0xFF009900);
		canvas.drawCircle(80 * coords[0] + 40, 80 * coords[1] + 40, 32, paint);
		if (coords[0] != coords[2] && coords[1] != coords[3])
			canvas.drawLine(coords[0] * 80 + (coords[2] - coords[0]) * SQRT512 + 40,
					coords[1] * 80 + (coords[3] - coords[1]) * SQRT512 + 40,
					coords[2] * 80 + 40, coords[3] * 80 + 40, paint);
		else if (coords[0] == coords[2])
			canvas.drawLine(80 * coords[0] + 40, 48 * coords[1] + 32 * coords[3] + 40,
					80 * coords[2] + 40, 80 * coords[3] + 40, paint);
		else
			canvas.drawLine(48 * coords[0] + 32 * coords[2] + 40, 80 * coords[1] + 40,
					80 * coords[2] + 40, 80 * coords[3] + 40, paint);
		for (int i = 2; i < coords.length - 2; i += 2)
			canvas.drawLine(80 * coords[i] + 40, 80 * coords[i + 1] + 40,
					80 * coords[i + 2] + 40, 80 * coords[i + 3] + 40, paint);
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
