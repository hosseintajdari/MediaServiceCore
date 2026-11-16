package com.liskovsoft.youtubeapi.videoinfo.models;

import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;
import com.liskovsoft.googlecommon.common.models.V2.TextItem;

public class TranslationLanguage {
    @JsonPath("$.languageCode")
    private String mLanguageCode;

    @JsonPath("$.languageName")
    private TextItem mLanguageName;
    private String mLanguageNameOverride;

    public TranslationLanguage() {
    }

    public TranslationLanguage(String languageCode, String languageName) {
        mLanguageCode = languageCode;
        mLanguageNameOverride = languageName;
    }

    public String getLanguageCode() {
        return mLanguageCode;
    }

    public String getLanguageName() {
        return mLanguageNameOverride != null ? mLanguageNameOverride : mLanguageName != null ? mLanguageName.toString() : null;
    }
}
