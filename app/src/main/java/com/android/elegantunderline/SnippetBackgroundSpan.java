package com.android.elegantunderline;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Spanned;

/**
 * This lets a span draw the background behind a snippet of text.
 *
 * To give this span effect, the entire paragraph must have a {@link SnippetBackgroundSpanShim}.
 */
public interface SnippetBackgroundSpan {

    void drawSnippetBackground(Canvas c, Paint p,
                               Spanned text, int snippetStart, int snippetEnd,
                               Rect snippetBounds, int baseline);

}
