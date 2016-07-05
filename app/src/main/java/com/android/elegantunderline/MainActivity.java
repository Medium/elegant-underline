package com.android.elegantunderline;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        for (int id: new int[] {R.id.underlined_by_span_0,
                                R.id.underlined_by_span_1,
                                R.id.underlined_by_span_2,
                                R.id.underlined_by_span_3}) {
            TextView underlinedBySpan = (TextView) findViewById(id);
            SpannableString spanned = new SpannableString(underlinedBySpan.getText());
            spanned.setSpan(SnippetBackgroundSpanShim.create(), 0, spanned.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spanned.setSpan(AnchorSpan.create(), 0, spanned.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            underlinedBySpan.setMovementMethod(LinkMovementMethod.getInstance());
            underlinedBySpan.setText(spanned);
        }
    }

}
