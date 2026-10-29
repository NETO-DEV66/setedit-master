package io.github.netodev66.setedit;

import androidx.annotation.Keep;

@Keep
public interface PropertyCallback {
    void handleProperty(String key, String value);
}
