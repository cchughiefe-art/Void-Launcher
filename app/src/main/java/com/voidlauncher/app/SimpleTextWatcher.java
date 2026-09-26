package com.voidlauncher.app;

import android.text.Editable;
import android.text.TextWatcher;
import java.util.function.Consumer;

final class SimpleTextWatcher implements TextWatcher {
    private final Consumer<String> changed;
    SimpleTextWatcher(Consumer<String> changed) { this.changed = changed; }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { changed.accept(s.toString()); }
    @Override public void afterTextChanged(Editable s) {}
}
