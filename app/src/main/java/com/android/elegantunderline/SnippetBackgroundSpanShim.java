package com.android.elegantunderline;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.text.style.LeadingMarginSpan.LeadingMarginSpan2;
import android.text.style.LineBackgroundSpan;
import android.text.style.MetricAffectingSpan;
import android.view.Gravity;
import android.widget.TextView;

/**
 * This span dispatches a {@link #drawBackground} call to the snippets of text spanned by
 * {@link SnippetBackgroundSpan}, so they can draw their own background.
 *
 * It uses a workaround for the broken LineBackgroundSpan
 * @see <a href="https://code.google.com/p/android/issues/detail?id=197281">Nick's bug report</a>
 */
public class SnippetBackgroundSpanShim implements LineBackgroundSpan {
    private final TextPaint workPaint = new TextPaint();

    public static SnippetBackgroundSpanShim create() {
        return new SnippetBackgroundSpanShim();
    }

    @Override public void drawBackground(
            Canvas c, Paint p, int left, int right, int top, int baseline, int bottom,
            CharSequence text, int lineStart, int lineEnd, int lineNum) {
        if (!(text instanceof Spanned)) {
            return;
        }

        Spanned spanned = (Spanned) text;
        SnippetBackgroundSpan[] snippets = spanned.getSpans(lineStart, lineEnd, SnippetBackgroundSpan.class);

        for (SnippetBackgroundSpan span: snippets) {
            drawSnippetBackground(span, c, p, left, right, top, baseline, bottom, spanned, lineStart, lineEnd, lineNum);
        }
    }

    /**
     * This calculates where the snippet starts (from the left) along with the width of the snippet.
     * It then invokes {@link SnippetBackgroundSpan#drawSnippetBackground} on the {@param span}.
     */
    private void drawSnippetBackground(
            SnippetBackgroundSpan span, Canvas c, Paint p, int left, int right, int top, int baseline, int bottom,
            Spanned text, int lineStart, int lineEnd, int lineNum) {
        int start = Math.max(text.getSpanStart(span), lineStart);
        int end = Math.min(text.getSpanEnd(span), lineEnd);
        if (start > end) {
            // Something very bad has happened. Just bail.
            return;
        }

        left += getParagraphLeadingMargin(text, lineStart, lineEnd, lineNum);
        left += getAlignmentLeftMargin(p, left, right, text, lineStart, lineEnd);

        float leftOffset = measureWidth(p, text, lineStart, start);
        float width = measureWidth(p, text, start, end);

        Rect snippetBounds = new Rect(left + (int)leftOffset, top, left + (int)(leftOffset + width), bottom);
        span.drawSnippetBackground(c, p, text, start, end, snippetBounds, baseline);
    }

    /** @param basePaint The base styles for the paragraph */
    private float measureWidth(Paint basePaint, Spanned text, int start, int end) {
        float width = 0;
        int currentEnd;
        for (int current = start; current < end; current = currentEnd) {
            currentEnd = text.nextSpanTransition(current, end, MetricAffectingSpan.class);
            MetricAffectingSpan[] spans = text.getSpans(current, currentEnd, MetricAffectingSpan.class);

            // Use work paint to compute the style for this particular span transition.
            workPaint.set(basePaint);
            for (MetricAffectingSpan span : spans) {
                span.updateMeasureState(workPaint);
            }
            width += workPaint.measureText(text, current, currentEnd);
        }
        return width;
    }

    /**
     * Returns the effective leading margin (unsigned) for this line,
     * taking into account LeadingMarginSpan and LeadingMarginSpan2.
     *
     * Adapted from android.text.Layout#getParagraphLeadingMargin
     */
    private int getParagraphLeadingMargin(Spanned spanned, int lineStart, int lineEnd, int lineNum) {
        LeadingMarginSpan[] spans = spanned.getSpans(lineStart, lineEnd, LeadingMarginSpan.class);
        if (spans.length == 0) {
            return 0; // no leading margin span;
        }

        int margin = 0;
        boolean isFirstParaLine = lineNum == 0;
        boolean useFirstLineMargin = isFirstParaLine;
        for (int i = 0; i < spans.length; i++) {
            if (spans[i] instanceof LeadingMarginSpan2) {
                int count = ((LeadingMarginSpan2) spans[i]).getLeadingMarginLineCount();
                // if there is more than one LeadingMarginSpan2, use the count that is greatest
                useFirstLineMargin |= lineNum < count;
            }
        }
        for (int i = 0; i < spans.length; i++) {
            LeadingMarginSpan span = spans[i];
            margin += span.getLeadingMargin(useFirstLineMargin);
        }

        return margin;
    }

    /**
     * Returns the effective leading margin (unsigned) for this line,
     * based on the paragraph alignment.
     */
    private int getAlignmentLeftMargin(
            Paint p, int left, int right, Spanned spanned, int lineStart, int lineEnd) {
        if (p.getTextAlign() == Paint.Align.CENTER) {
            return Math.round(((right - left) - p.measureText(spanned, lineStart, lineEnd)) / 2.0f);
        }
        return 0;
    }
}
