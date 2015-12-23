package net.pietu1998.wordbasehacker;

import net.pietu1998.wordbasehacker.solver.Coordinate;
import net.pietu1998.wordbasehacker.solver.Possibility;
import net.pietu1998.wordbasehacker.solver.Tile;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

public class BoardDrawable extends Drawable {

	private Possibility pos;
	private boolean flipped;

	private final static float SQRT512 = (float) (16 * Math.sqrt(2));

	public BoardDrawable(Possibility pos, boolean flipped) {
		this.pos = pos;
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
	public void draw(Canvas canvas) {
		if (pos.getResult() == null)
			return;

		Paint whiteBg = new Paint();
		whiteBg.setStyle(Paint.Style.FILL);
		whiteBg.setColor(0xFFFFFFFF);
		Paint orangeBg = new Paint(whiteBg);
		orangeBg.setColor(0xFFFF6600);
		Paint blueBg = new Paint(whiteBg);
		blueBg.setColor(0xFF66FFFF);
		Paint blackBg = new Paint(whiteBg);
		blackBg.setColor(0xFF000000);

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

		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 13; y++) {
				Tile t = pos.getResult().getTiles()[x][y];
				if (t.isSet(Tile.MINE))
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, blackBg);
				else if (t.isSet(Tile.PLAYER))
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, flipped ? blueBg : orangeBg);
				else if (t.isSet(Tile.OPPONENT))
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, flipped ? orangeBg : blueBg);
				else
					canvas.drawRect(x * 80, y * 80, x * 80 + 80, y * 80 + 80, whiteBg);
				canvas.drawText(String.valueOf(t.getLetter()),
						x * 80 + 40 - blackText.measureText(String.valueOf(t.getLetter())) / 2,
						y * 80 + 40 - blackText.ascent() / 2, t.isSet(Tile.MINE) ? whiteText : blackText);
			}
		}
		if (pos.getCoordinates().length > 1) {
			Coordinate[] c = pos.getCoordinates();
			canvas.drawCircle(80 * c[0].x + 40, 80 * c[0].y + 40, 32, path);
			if (c[0].x != c[1].x && c[0].y != c[1].y)
				canvas.drawLine(c[0].x * 80 + (c[1].x - c[0].x) * SQRT512 + 40, c[0].y * 80 + (c[1].y - c[0].y)
						* SQRT512 + 40, c[1].x * 80 + 40, c[1].y * 80 + 40, path);
			else if (c[0].x == c[1].x)
				canvas.drawLine(80 * c[0].x + 40, 48 * c[0].y + 32 * c[1].y + 40, 80 * c[1].x + 40, 80 * c[1].y + 40,
						path);
			else
				canvas.drawLine(48 * c[0].x + 32 * c[1].x + 40, 80 * c[0].y + 40, 80 * c[1].x + 40, 80 * c[1].y + 40,
						path);
			for (int i = 1; i < c.length - 1; i++) {
				canvas.drawLine(80 * c[i].x + 40, 80 * c[i].y + 40, 80 * c[i + 1].x + 40, 80 * c[i + 1].y + 40, path);
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
