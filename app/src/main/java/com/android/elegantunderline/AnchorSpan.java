package com.android.elegantunderline;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

/**
 * This span draws a link-like underline beneath text.
 *
 * On pre-KitKat, it falls back to using {@link Paint#setUnderlineText}.
 */
public class AnchorSpan extends ClickableSpan implements SnippetBackgroundSpan {

    // I cribbed and tweaked the underline offset and width from
    // https://github.com/google/skia/blob/4d51f64ff18e2e15c40fec0c374d89879ba273bc/src/core/SkTextFormatParams.h#L18-L20
    // note: all of the sizes are scaled to the text size
    public static final float UNDERLINE_OFFSET = 1f/9f;
    public static final float UNDERLINE_STROKE_WIDTH = 1/18f;
    public static final float DISTANCE_FROM_LETTERS = 1/9f;
    public static final int UNDERLINE_ALPHA = 128; // @IntRange(from = 0, to = 255)

    public static AnchorSpan create() {
        return new AnchorSpan();
    }

    /**
     * On pre-KitKat, this provides fallback to an ugly underline using the text paint.
     */
    @Override
    public void updateDrawState(TextPaint ds) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            ds.setUnderlineText(true);
        }
    }

    @Override
    public void onClick(View widget) {
        state = DemoState.values()[(state.ordinal() + 1) % DemoState.values().length];
        Toast.makeText(widget.getContext(), state.description, Toast.LENGTH_SHORT).show();
    }

    enum DemoState {
        NONE("no underline"),
        SIMPLE_UNDERLINE("simple underline"),
        WIDE_UNDERLINE("wider underline to give space"),
        INTERSECTIONS("find intersections"),
        ELEGANT_UNDERLINE("elegant underline (remove intersections)");

        String description;

        DemoState(String description) {
            this.description = description;
        }
    }

    DemoState state = DemoState.ELEGANT_UNDERLINE;


    /**
     * On KitKat+, this draws a pretty underline with spacing around the descenders.
     */
    @Override
    public void drawSnippetBackground(Canvas c, Paint p,
                                      Spanned text, int snippetStart, int snippetEnd,
                                      Rect snippetBounds, int baseline) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        Path strokedOutline = new Path();
        Path outline = new Path();
        Path cutUnderline = new Path();
        Path drawUnderline = new Path();
        Paint stroked = new Paint(p);
        Paint underlinePaint = new Paint(p);

        // scale configuration to the text size
        float textSize = p.getTextSize();
        float underlineOffset = UNDERLINE_OFFSET * textSize;
        float underlineStrokeWidth = UNDERLINE_STROKE_WIDTH * textSize;
        float distanceFromLetters = DISTANCE_FROM_LETTERS * textSize;

        float cutWidth = underlineStrokeWidth + 2f * distanceFromLetters;
        int underlineAlpha = UNDERLINE_ALPHA;

        p.getTextPath(text.toString(), snippetStart, snippetEnd, 0.0f, 0.0f, outline);
        outline.offset(snippetBounds.left, 0);

        underlinePaint.setAntiAlias(true);
        underlinePaint.setAlpha(underlineAlpha);

        // this is the widened stroke that we use to create the void spaces
        stroked.setStyle(Paint.Style.FILL_AND_STROKE);
        stroked.setStrokeWidth(distanceFromLetters);
        stroked.setStrokeCap(Paint.Cap.BUTT);

        cutUnderline.addRect(
                snippetBounds.left, underlineOffset - cutWidth / 2f,
                snippetBounds.right, underlineOffset + cutWidth / 2f,
                Path.Direction.CW);
        drawUnderline.addRect(
                snippetBounds.left, underlineOffset - underlineStrokeWidth / 2f,
                snippetBounds.right, underlineOffset + underlineStrokeWidth / 2f,
                Path.Direction.CW);
        Path demoDrawUnderlineRaw = new Path(drawUnderline); // capture the raw underline for the demo

        // find the intersections: intersects the text with the underline
        outline.op(cutUnderline, Path.Op.INTERSECT);

        // Stroke the clipped text outline and get the result as a fill path
        stroked.getFillPath(outline, strokedOutline);

        // Subtract the stroked outline from the underline
        drawUnderline.op(strokedOutline, Path.Op.DIFFERENCE);

        // offset to the proper location
        drawUnderline.offset(0, baseline);

        // outside of a demo, right here we would just: c.drawPath(drawUnderline, underlinePaint);

        // offset the other things we're going to draw for the demo
        demoDrawUnderlineRaw.offset(0, baseline);
        cutUnderline.offset(0, baseline);
        strokedOutline.offset(0, baseline);

        // draw only the thing we're showing for this state of the demo
        Paint demoPaint = new Paint(underlinePaint);
        demoPaint.setColor(Color.parseColor("#7057AD68"));
        stroked.setColor(Color.parseColor("#7057AD68"));
        if (state == DemoState.INTERSECTIONS) {
            c.drawPath(strokedOutline, stroked);
        } else if (state == DemoState.SIMPLE_UNDERLINE) {
            c.drawPath(demoDrawUnderlineRaw, underlinePaint);
        } else if (state == DemoState.WIDE_UNDERLINE) {
            c.drawPath(cutUnderline, demoPaint);
        } else if (state == DemoState.ELEGANT_UNDERLINE) {
            c.drawPath(drawUnderline, underlinePaint);
        }
    }
}
